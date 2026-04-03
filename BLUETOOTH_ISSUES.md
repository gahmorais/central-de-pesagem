# Inconsistências no Módulo Bluetooth e Impressora Honeywell — Central de Pesagem

> Gerado em: 2026-04-03
> Branch analisada: `pesagem/add_tare`
> Arquivos analisados:
> - `bluetooth/…/BluetoothConnected.kt`
> - `bluetooth/…/BluetoothHandler.kt`
> - `app/…/ui/viewmodel/MainViewModel.kt`
> - `app/…/ui/fragments/MainFragment.kt`
> - `app/…/printers/HoneywellPrinter.kt`

---

## Índice

| # | Título | Severidade | Arquivo Principal |
|---|--------|-----------|-------------------|
| 1 | Canal de peso único compartilhado entre dispositivos | ALTA | MainViewModel.kt |
| 2 | Sem sincronização nos streams do socket | ALTA | BluetoothConnected.kt |
| 3 | Socket não fechado ao falhar no `read()` | ALTA | BluetoothConnected.kt |
| 4 | Acesso a `device.uuids[0]` sem verificação | ALTA | BluetoothHandler.kt |
| 5 | Race condition na lista de dispositivos do spinner | ALTA | MainFragment.kt |
| 6 | Múltiplas escritas concorrentes no mesmo socket | ALTA | MainViewModel.kt |
| 7 | Lógica de permissão invertida no `print()` | ALTA | MainFragment.kt |
| 8 | Spinner recebe objetos `BluetoothConnected` em vez de strings | ALTA | MainFragment.kt |
| 9 | Estado da tara alterado antes de confirmar escrita | MÉDIA | MainViewModel.kt |
| 10 | Sockets não fechados no `onCleared()` | MÉDIA | MainViewModel.kt |
| 11 | Busy-wait sem delay quando não há dados | MÉDIA | BluetoothConnected.kt |
| 12 | `ClosedSocketException` com semântica incorreta | MÉDIA | BluetoothConnected.kt |
| 13 | Flow continua após ViewModel destruído | MÉDIA | MainViewModel.kt |
| 14 | IOException original perdida no catch | MÉDIA | BluetoothConnected.kt |
| 15 | `mapIndexed` com resultado descartado | BAIXA | MainFragment.kt |
| 16 | ConcurrentModificationException em `jobsRunning` | BAIXA | MainViewModel.kt |

---

---

## Issue #1 — Canal de peso único compartilhado entre dispositivos

**Severidade:** ALTA
**Arquivos:** `MainViewModel.kt` linhas 101, 237–254, 390–393

### Descrição

Todos os dispositivos Bluetooth conectados escrevem no mesmo `weightReadyChannel`. Quando `getWeight()` é chamado para um dispositivo específico, ele pode receber dados enviados por outro dispositivo, retornando um peso incorreto sem qualquer aviso.

```kotlin
// MainViewModel.kt:101
private val weightReadyChannel = Channel<Double>(Channel.UNLIMITED)

// MainViewModel.kt:390–393 — qualquer device dispara updateWeight()
private fun updateWeight(message: ByteArray) {
    val newWeight: Double = Formatter.formatFromToledoProtocolP05(message)
    weightReadyChannel.trySend(newWeight)  // <-- canal único para todos
}

// MainViewModel.kt:245–246 — não sabe de qual device veio
val newWeight = withTimeoutOrNull(3000L) {
    weightReadyChannel.receive()
}
```

### Passos para corrigir

1. **Substituir o canal único por um mapa de canais por dispositivo.**
   Em `MainViewModel.kt`, trocar:
   ```kotlin
   private val weightReadyChannel = Channel<Double>(Channel.UNLIMITED)
   ```
   por:
   ```kotlin
   private val weightChannels = mutableMapOf<BluetoothConnected, Channel<Double>>()
   ```

2. **Criar canal ao registrar cada dispositivo em `startCollectData()`** (linha 320):
   ```kotlin
   private fun startCollectData(device: BluetoothConnected) {
       weightChannels[device] = Channel(Channel.UNLIMITED)
       val job = device.read().onEach { bytes ->
           // ... lógica de status existente ...
           else -> updateWeight(device, bytes)   // passar device
       }...
   }
   ```

3. **Atualizar `updateWeight()` para receber o device como parâmetro:**
   ```kotlin
   private fun updateWeight(device: BluetoothConnected, message: ByteArray) {
       val newWeight = Formatter.formatFromToledoProtocolP05(message)
       weightChannels[device]?.trySend(newWeight)
   }
   ```

4. **Atualizar `getWeight()` para consumir do canal correto:**
   ```kotlin
   private suspend fun getWeight(btConnected: BluetoothConnected): Double {
       val channel = weightChannels[btConnected]
           ?: throw Exception("Canal não encontrado para o dispositivo")
       val command = byteArrayOf(0x05)
       btConnected.write(command)
       channel.tryReceive().getOrNull()   // limpar valor anterior
       val newWeight = withTimeoutOrNull(3000L) { channel.receive() }
       return newWeight ?: throw Exception(resourceProvider.getString(R.string.weight_timeout_exceed))
   }
   ```

5. **Fechar e remover o canal ao desconectar o dispositivo:**
   ```kotlin
   private fun removeDevice(device: BluetoothConnected) {
       weightChannels[device]?.close()
       weightChannels.remove(device)
   }
   ```

6. **No `onCleared()`, fechar todos os canais:**
   ```kotlin
   override fun onCleared() {
       super.onCleared()
       weightChannels.values.forEach { it.close() }
       weightChannels.clear()
       cancelAllJobs()
       epson.finalizePrinter()
   }
   ```

---

## Issue #2 — Sem sincronização nos streams do socket

**Severidade:** ALTA
**Arquivo:** `BluetoothConnected.kt` linhas 15–53

### Descrição

`write()` e `read()` acessam `outputStream` e `inputStream` do mesmo `BluetoothSocket` em threads diferentes sem nenhuma sincronização. Escritas simultâneas (ex: enviar tara enquanto lê peso) corrompem o fluxo de dados do socket.

```kotlin
// Thread 1 — write() chamado de changeTare() (linha 301 do MainViewModel)
socket.outputStream.write(data)

// Thread 2 — read() rodando em Dispatchers.IO (flowOn linha 52)
numBytes = socket.inputStream.read(buffer)

// BluetoothSocket não é thread-safe — corrida de dados
```

### Passos para corrigir

1. **Adicionar um `Mutex` na classe `BluetoothConnected`:**
   ```kotlin
   import kotlinx.coroutines.sync.Mutex
   import kotlinx.coroutines.sync.withLock

   class BluetoothConnected(private val socket: BluetoothSocket) {
       private val writeMutex = Mutex()
   ```

2. **Tornar `write()` uma suspend function e protegê-la com o Mutex:**
   ```kotlin
   suspend fun write(data: ByteArray) {
       writeMutex.withLock {
           try {
               socket.outputStream.write(data)
           } catch (e: IOException) {
               // tratamento existente
           }
       }
   }
   ```

3. **Atualizar todos os callers de `write()` no `MainViewModel.kt`** — as chamadas nas linhas 239, 301, 317 e 613 precisam ser feitas de dentro de uma coroutine (já estão, na maior parte dos casos). Verificar a linha 613 (`loadPrice`) que é chamada de função não-suspend.

4. **Na linha 613 de `MainViewModel.kt`**, converter `loadPrice` para suspend ou usar `viewModelScope.launch`:
   ```kotlin
   private fun loadPrice(value: Double) {
       val priceByteArray = Formatter.formatDataToLoadPriceToledo(value)
       viewModelScope.launch {
           _activeDevice.value?.write(priceByteArray)
       }
   }
   ```

---

## Issue #3 — Socket não fechado ao falhar no `read()`

**Severidade:** ALTA
**Arquivo:** `BluetoothConnected.kt` linhas 34–53

### Descrição

O bloco `.catch` no flow de leitura relança a exceção sem fechar o socket. Cada falha de leitura deixa um socket aberto, acumulando recursos vazados.

```kotlin
fun read(): Flow<ByteArray> {
    return flow {
        // ...
    }.catch { e ->
        e.printStackTrace()
        throw e   // <-- socket nunca fechado!
    }.flowOn(Dispatchers.IO)
}
```

### Passos para corrigir

1. **Adicionar método `disconnect()` em `BluetoothConnected`:**
   ```kotlin
   fun disconnect() {
       try {
           socket.close()
       } catch (e: Exception) {
           Timber.e(e, "Erro ao fechar socket")
       }
   }
   ```

2. **Fechar o socket no `.catch` do flow:**
   ```kotlin
   }.catch { e ->
       Timber.e(e, "Erro na leitura do socket")
       disconnect()
       throw e
   }
   ```

3. **Adicionar `isConnected` como propriedade:**
   ```kotlin
   val isConnected: Boolean get() = socket.isConnected
   ```

4. **No `MainViewModel.kt`, capturar a exceção relançada em `startCollectData()` (linha 340) e remover o device da lista:**
   ```kotlin
   .catch { e ->
       e.message?.let { showMessage(it) }
       removeDevice(device)   // remover da lista _devices
   }
   ```

---

## Issue #4 — Acesso a `device.uuids[0]` sem verificação

**Severidade:** ALTA
**Arquivo:** `BluetoothHandler.kt` linha 16–17

### Descrição

`device.uuids` pode ser `null` ou vazio em dispositivos que ainda não foram totalmente consultados via SDP. O acesso direto ao índice `[0]` causa crash.

```kotlin
// BluetoothHandler.kt:16–17
Timber.d("Conectando ao dispositivo ${device.uuids[0].uuid}")  // NPE ou IndexOutOfBoundsException
val socket = device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
```

### Passos para corrigir

1. **Adicionar constante de UUID padrão SPP (Serial Port Profile):**
   ```kotlin
   import java.util.UUID

   private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
   ```

2. **Substituir o acesso direto por leitura segura com fallback:**
   ```kotlin
   suspend fun connectToDevice(device: BluetoothDevice): BluetoothConnected = try {
       val uuid = device.uuids
           ?.firstOrNull()
           ?.uuid
           ?: SPP_UUID
       Timber.d("Conectando ao dispositivo ${device.name} com UUID $uuid")
       val socket = device.createRfcommSocketToServiceRecord(uuid)
       socket.connect()
       BluetoothConnected(socket)
   } catch (e: IOException) {
       e.printStackTrace()
       throw e
   }
   ```

3. **Em `connectToDevices()` no `MainViewModel.kt` (linha 364)**, tratar o caso de falha em um device sem impedir a conexão dos demais:
   ```kotlin
   devices.forEach { device ->
       try {
           val socket = withContext(Dispatchers.IO) {
               bluetoothHandler.connectToDevice(device)
           }
           newDevices.add(socket)
           startCollectData(socket)
       } catch (e: Exception) {
           Timber.e(e, "Falha ao conectar dispositivo ${device.name}")
           emitMessage("Dispositivo ${device.name} não pôde ser conectado")
       }
   }
   ```

---

## Issue #5 — Race condition na lista de dispositivos do spinner

**Severidade:** ALTA
**Arquivo:** `MainFragment.kt` linhas 179–218

### Descrição

O observer de `devices` (linha 181) atualiza o adapter do spinner ao mesmo tempo em que `onItemSelected` (linha 200) lê `mainViewModel.devices.value` e usa o índice `i` para acessar a lista. Se a lista for atualizada entre a leitura e o acesso, o índice `i` pode estar fora dos limites.

```kotlin
// MainFragment.kt:211 — sem bounds check
mainViewModel.setActiveDevice(devices[i])   // IndexOutOfBoundsException!
```

### Passos para corrigir

1. **Adicionar verificação de bounds antes do acesso no `onItemSelected`:**
   ```kotlin
   override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, i: Int, p3: Long) {
       val devices = mainViewModel.devices.value
       if (i < 0 || i >= devices.size) return
       mainViewModel.setActiveDevice(devices[i])
   }
   ```

2. **Remover o `ArrayAdapter` duplicado criado dentro do `onItemSelected`** (linhas 202–210). Ele não é necessário — o adapter já é configurado pelo observer em `configureObservers()`. Simplificar para:
   ```kotlin
   override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, i: Int, p3: Long) {
       val devices = mainViewModel.devices.value
       if (i >= 0 && i < devices.size) {
           mainViewModel.setActiveDevice(devices[i])
       }
   }
   ```

3. **No observer de `configureObservers()` (linha 181)**, usar a lista mapeada com nomes no adapter (ver Issue #8).

---

## Issue #6 — Múltiplas escritas concorrentes no mesmo socket

**Severidade:** ALTA
**Arquivo:** `MainViewModel.kt` linhas 270–271, 301, 317, 613

### Descrição

Várias operações podem chamar `btConnected.write()` simultaneamente sobre o mesmo socket:
- `weighining()` chama `loadTare()` → `write()` (linha 317)
- `changeTare()` chama `write()` (linha 301)
- `loadPrice()` chama `write()` (linha 613)
- Tudo isso enquanto o flow de `read()` processa dados da mesma conexão

O `BluetoothSocket` não é thread-safe. Escritas simultâneas corrompem a stream de dados.

### Passos para corrigir

Este issue é resolvido em conjunto com o Issue #2 (adicionar `Mutex` em `BluetoothConnected`). Além disso:

1. **Garantir que `loadTare()` (linha 310) seja suspend** para poder aguardar o mutex:
   ```kotlin
   private suspend fun loadTare(btConnected: BluetoothConnected?, value: Double) {
       if (btConnected == null) {
           showMessage("Balança não pareada")
           return
       }
       val newTare = Formatter.formatDataToLoadTareToledo(value)
       btConnected.write(newTare)   // agora protegido pelo Mutex
   }
   ```

2. **Confirmar que `loadPrice()` (linha 611) nunca é chamado durante uma pesagem ativa.** Avaliar se o envio de preço pode ser desacoplado do fluxo de pesagem ou serializado.

3. **Sequenciar explicitamente as operações em `weighining()` (linha 270)**:
   ```kotlin
   loadTare(btConnected, _currentTare.value ?: 0.0)
   delay(500)                         // já presente — aguarda processamento
   val newWeight = getWeight(btConnected)
   // só depois chamar loadPrice
   loadPrice(currentProduct.price)
   ```

---

## Issue #7 — Lógica de permissão invertida no `print()`

**Severidade:** ALTA
**Arquivo:** `MainFragment.kt` linhas 233–245

### Descrição

O `if (!granted)` está invertido: a impressão é chamada quando a permissão é **negada**, e o erro é exibido quando é **concedida**. Isso faz com que a impressão nunca funcione normalmente.

```kotlin
// MainFragment.kt:239–244 — lógica invertida
if (!granted) {
    lifecycleScope.launch { mainViewModel.print() }  // imprime quando NEGADO
} else {
    error(getString(R.string.fail_to_access_storage))  // erro quando CONCEDIDO
}
```

### Passos para corrigir

1. **Inverter a condição** na função `print()` do `MainFragment.kt`:
   ```kotlin
   .checkPermission { granted ->
       Timber.d("Granted:$granted")
       if (granted) {
           lifecycleScope.launch { mainViewModel.print() }
       } else {
           error(getString(R.string.fail_to_access_storage))
       }
   }
   ```

> **Nota:** A partir do Android 10+ (API 29), a permissão `WRITE_EXTERNAL_STORAGE` é desnecessária para acesso à pasta da aplicação. Verificar se essa permissão é realmente necessária no contexto de impressão ou se pode ser removida.

---

## Issue #8 — Spinner exibe objetos `BluetoothConnected` em vez de nomes

**Severidade:** ALTA
**Arquivo:** `MainFragment.kt` linhas 179–192

### Descrição

O resultado do `mapIndexed` que gera os nomes `"Balança 1"`, `"Balança 2"` etc. é descartado. O `ArrayAdapter` recebe a lista de objetos `BluetoothConnected` e chama `.toString()` em cada um, exibindo o endereço de memória no lugar do nome.

```kotlin
// MainFragment.kt:182–191
mainViewModel.devices.onEach { devices ->
    devices.mapIndexed { i, _ -> "Balança ${i + 1}" }  // resultado descartado!
    ArrayAdapter(
        requireActivity(),
        android.R.layout.simple_spinner_item,
        devices   // <-- BluetoothConnected objects, não strings
    )...
}
```

### Passos para corrigir

1. **Usar a lista mapeada de nomes no adapter:**
   ```kotlin
   mainViewModel.devices.onEach { devices ->
       val deviceNames = devices.mapIndexed { i, _ -> "Balança ${i + 1}" }
       ArrayAdapter(
           requireActivity(),
           android.R.layout.simple_spinner_item,
           deviceNames
       ).also { adapter ->
           adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
           binding.spinnerScale.adapter = adapter
       }
   }.launchIn(lifecycleScope)
   ```

2. **Remover o `ArrayAdapter` duplicado dentro de `onItemSelected`** (linhas 202–210 em `configureSpinner()`), que também cria um adapter com nomes mas sobrescreve o adapter correto, gerando comportamento inconsistente. O spinner não precisa ter seu adapter recriado a cada seleção.

---

## Issue #9 — Estado da tara alterado antes de confirmar escrita

**Severidade:** MÉDIA
**Arquivo:** `MainViewModel.kt` linhas 291–308

### Descrição

`_isActiveTare` é togglado na linha 293 antes de `getWeight()` ou `write()` serem executados com sucesso. Se qualquer operação falhar, o estado da UI fica incorreto sem rollback.

```kotlin
// MainViewModel.kt:293–307
_isActiveTare.value = !_isActiveTare.value   // toggle antecipado
val weight = if (_isActiveTare.value) {
    getWeight(btConnected)                     // pode lançar exceção
} else { 0.0 }
btConnected.write(tareBytes)                  // pode lançar exceção
_currentTare.update { weight }
// catch não faz rollback do _isActiveTare
```

### Passos para corrigir

1. **Calcular o próximo estado antes de aplicar e fazer rollback em caso de erro:**
   ```kotlin
   suspend fun changeTare(btConnected: BluetoothConnected) {
       val previousTareState = _isActiveTare.value
       val newTareState = !previousTareState
       try {
           val weight = if (newTareState) {
               getWeight(btConnected)
           } else {
               0.0
           }
           val tareBytes = Formatter.formatDataToLoadTareToledo(weight)
           btConnected.write(tareBytes)

           // só atualiza estado após sucesso
           _isActiveTare.value = newTareState
           _currentTare.update { weight }
           Timber.i("Tara aplicada: $weight kg")
       } catch (e: ClosedSocketException) {
           // estado permanece inalterado (previousTareState)
           e.message?.let { emitMessage(it) }
       } catch (e: Exception) {
           e.message?.let { emitMessage(it) }
       }
   }
   ```

---

## Issue #10 — Sockets não fechados no `onCleared()`

**Severidade:** MÉDIA
**Arquivo:** `MainViewModel.kt` linhas 353–358

### Descrição

`onCleared()` cancela as coroutines e fecha o `weightReadyChannel`, mas não chama `socket.close()` nos objetos `BluetoothConnected`. Os sockets permanecem abertos mesmo após o ViewModel ser destruído.

```kotlin
// MainViewModel.kt:353–358
override fun onCleared() {
    super.onCleared()
    cancelAllJobs()          // cancela jobs, mas não fecha sockets
    weightReadyChannel.close()
    epson.finalizePrinter()
    // _devices.value ainda contém sockets abertos!
}
```

### Passos para corrigir

1. **Adicionar método `disconnect()` em `BluetoothConnected`** (ver Issue #3).

2. **Iterar sobre os devices em `onCleared()` e desconectá-los:**
   ```kotlin
   override fun onCleared() {
       super.onCleared()
       cancelAllJobs()
       _devices.value.forEach { device ->
           device.disconnect()
       }
       weightChannels.values.forEach { it.close() }   // ver Issue #1
       weightChannels.clear()
       epson.finalizePrinter()
   }
   ```

3. **Criar método `disconnectAll()` no ViewModel** para reutilização (ex: quando usuário solicita reconexão):
   ```kotlin
   fun disconnectAll() {
       cancelAllJobs()
       _devices.value.forEach { it.disconnect() }
       _devices.update { emptyList() }
       _activeDevice.update { null }
   }
   ```

---

## Issue #11 — Busy-wait sem delay quando não há dados

**Severidade:** MÉDIA
**Arquivo:** `BluetoothConnected.kt` linhas 38–47

### Descrição

O `delay(100)` só é executado quando `available()` retorna dados. Quando não há dados, o loop reinicia imediatamente sem nenhuma pausa, consumindo CPU desnecessariamente em 100% de um thread do IO dispatcher.

```kotlin
while (true) {
    numBytes = socket.inputStream.available()
    if (numBytes != 0) {
        delay(100)              // delay só quando há dados
        numBytes = socket.inputStream.read(buffer)
        // ...
    }
    // sem delay quando numBytes == 0 → CPU spinning!
}
```

### Passos para corrigir

1. **Adicionar um `delay` no ramo em que não há dados, ou mover o delay para fora do `if`:**
   ```kotlin
   while (true) {
       numBytes = socket.inputStream.available()
       if (numBytes != 0) {
           numBytes = socket.inputStream.read(buffer)
           val data = buffer.copyOf(numBytes)
           if (data.size > 1) {
               emit(data)
           }
       } else {
           delay(50)   // evita busy-wait; ajustar conforme latência aceitável
       }
   }
   ```

2. **Alternativa mais eficiente:** usar `inputStream.read(buffer)` diretamente (chamada bloqueante), sem `available()`, já que o flow já roda em `Dispatchers.IO`. A chamada bloqueante libera o thread enquanto aguarda dados:
   ```kotlin
   while (true) {
       numBytes = socket.inputStream.read(buffer)   // bloqueia até ter dados
       if (numBytes > 1) {
           emit(buffer.copyOf(numBytes))
       }
   }
   ```

> A abordagem com `read()` bloqueante é preferível pois é cooperativa com o sistema operacional, ao contrário do polling com `available()`.

---

## Issue #12 — `ClosedSocketException` com semântica incorreta

**Severidade:** MÉDIA
**Arquivo:** `BluetoothConnected.kt` linhas 15–32

### Descrição

A `ClosedSocketException` é lançada quando o socket **falha ao fechar**, não quando ele está fechado. A nomenclatura confunde quem trata o erro — o `MainViewModel.kt` captura essa exceção esperando indicar "socket fechado", mas na realidade indica "falha no fechamento".

```kotlin
// BluetoothConnected.kt:24–28
try {
    socket.close()
} catch (e: Exception) {
    throw ClosedSocketException("Não foi possível encerrar o socket")
    // semântica errada: lançada quando close() falha, não quando socket está fechado
}
```

Adicionalmente, a `IOException` original (causa raiz da falha de escrita) é impressa mas não encadeada na nova exceção, perdendo o contexto.

### Passos para corrigir

1. **Criar uma exceção de semântica correta, ou renomear `ClosedSocketException` para `SocketCloseFailureException`:**
   ```kotlin
   // exceptions/SocketCloseFailureException.kt
   class SocketCloseFailureException(message: String, cause: Throwable? = null)
       : Exception(message, cause)
   ```

2. **Encadear a causa original ao lançar a nova exceção em `write()`:**
   ```kotlin
   catch (e: IOException) {
       Timber.e(e, "Falha ao escrever no socket")
       try {
           socket.close()
       } catch (closeEx: Exception) {
           throw SocketCloseFailureException("Falha ao fechar socket após erro de escrita", closeEx)
       }
       throw e   // relançar IOException original se close() funcionou
   }
   ```

3. **Atualizar os `catch` no `MainViewModel.kt`** (linhas 284, 303) para tratar os dois casos separadamente.

---

## Issue #13 — Flow continua após ViewModel destruído

**Severidade:** MÉDIA
**Arquivo:** `MainViewModel.kt` linhas 320–358

### Descrição

O flow `device.read()` contém um loop `while(true)`. Quando o ViewModel é destruído e `job.cancel()` é chamado, o cancelamento só ocorre no próximo ponto de suspensão. Se `delay(100)` não for alcançado rapidamente (ex: `inputStream.available()` bloqueante), o cancelamento pode demorar ou nem ocorrer, deixando a coroutine ativa após `onCleared()`.

```kotlin
// BluetoothConnected.kt:38–47
while (true) {
    numBytes = socket.inputStream.available()   // pode bloquear; não verifica cancelamento
    // ...
    delay(100)   // único ponto de cancelamento cooperativo, e só quando há dados
}
```

### Passos para corrigir

1. **Adicionar verificação explícita de cancelamento no loop** com `currentCoroutineContext().isActive` ou `ensureActive()`:
   ```kotlin
   import kotlinx.coroutines.currentCoroutineContext
   import kotlinx.coroutines.isActive

   while (currentCoroutineContext().isActive) {
       numBytes = socket.inputStream.available()
       if (numBytes != 0) {
           // ...
       } else {
           delay(50)   // ponto de suspensão também funciona como ponto de cancelamento
       }
   }
   ```

2. **Ou, ao adotar a alternativa de `read()` bloqueante (Issue #11)**, garantir que fechar o socket (via `disconnect()`) interrompa o `read()` bloqueante, que por sua vez encerra o loop e o flow.

3. **No `onCleared()`, fechar os sockets antes de cancelar os jobs**, garantindo que qualquer `read()` bloqueante seja interrompido pelo fechamento:
   ```kotlin
   override fun onCleared() {
       super.onCleared()
       _devices.value.forEach { it.disconnect() }  // interrompe read() bloqueante
       cancelAllJobs()
       epson.finalizePrinter()
   }
   ```

---

## Issue #14 — IOException original perdida no catch

**Severidade:** MÉDIA
**Arquivo:** `BluetoothConnected.kt` linhas 18–28

### Descrição

Ao capturar uma `IOException` em `write()`, apenas `e.printStackTrace()` é chamado e o detalhe do erro (ex: `ECONNRESET`, `EPIPE`, `Broken pipe`) é descartado. A exceção relançada é de tipo diferente e sem causa encadeada.

```kotlin
catch (e: IOException) {
    e.printStackTrace()           // imprime mas não propaga informação
    Timber.d("Não foi possível enviar os dados")
    try {
        socket.close()
    } catch (e: Exception) {
        throw ClosedSocketException(...)   // causa original perdida
    }
}
```

### Passos para corrigir

1. **Usar `Timber.e(e, ...)` em vez de `Timber.d()`** para preservar o stack trace nos logs de produção:
   ```kotlin
   Timber.e(e, "Falha ao enviar dados via socket")
   ```

2. **Encadear a causa original** ao lançar qualquer nova exceção (ver Issue #12, passo 2).

3. **Usar nível de log adequado** — `Timber.d` é nível DEBUG e não aparece em builds de produção. Erros de I/O em hardware devem ser `Timber.e` (ERROR) ou `Timber.w` (WARNING).

---

## Issue #15 — `mapIndexed` com resultado descartado

**Severidade:** BAIXA
**Arquivo:** `MainFragment.kt` linha 182

### Descrição

O `mapIndexed` cria uma lista de strings `["Balança 1", "Balança 2", ...]` mas o resultado não é atribuído a nenhuma variável. É código morto.

```kotlin
devices.mapIndexed { i, _ -> "Balança ${i + 1}" }  // resultado descartado
```

### Passos para corrigir

Este issue é corrigido como parte do Issue #8. Ao atribuir o resultado do `mapIndexed` ao adapter, o código morto desaparece e o bug de exibição é corrigido simultaneamente.

---

## Issue #16 — Risco de `ConcurrentModificationException` em `jobsRunning`

**Severidade:** BAIXA
**Arquivo:** `MainViewModel.kt` linhas 106, 344, 347–351

### Descrição

`jobsRunning` é um `mutableListOf<Job>()` acessado por `add()` (linha 344) e `forEach()` (linha 348) sem sincronização. Em cenários com múltiplos dispositivos conectando simultaneamente, as chamadas concorrentes a `startCollectData()` podem causar `ConcurrentModificationException`.

```kotlin
private val jobsRunning = mutableListOf<Job>()   // não thread-safe

private fun startCollectData(device: BluetoothConnected) {
    val job = device.read()...launchIn(viewModelScope)
    jobsRunning.add(job)   // add concorrente possível
}

private fun cancelAllJobs() {
    jobsRunning.forEach { job ->   // iteração concorrente a add
        job.cancel()
    }
}
```

### Passos para corrigir

1. **Trocar por `CopyOnWriteArrayList` para acesso thread-safe sem locks explícitos:**
   ```kotlin
   import java.util.concurrent.CopyOnWriteArrayList

   private val jobsRunning = CopyOnWriteArrayList<Job>()
   ```

2. **Alternativa:** como o `viewModelScope` despacha no Main thread por padrão, a chance de concorrência real é baixa. Confirmar se `connectToDevices()` é sempre chamado da Main thread; se sim, a mudança é precautória mas recomendada.

---

---

---

# Inconsistências em HoneywellPrinter.kt

> Arquivo: `app/src/main/java/br/com/backupautomacao/pesagem_modular/printers/HoneywellPrinter.kt`

---

## Issue #H1 — `disconnect()` zera `usbDevice`, quebrando reconexões

**Severidade:** ALTA
**Linhas:** 194–207 (disconnect), 162–175 (connectPrinter)

### Descrição

`disconnect()` define `usbDevice = null` na linha 205. Após isso, qualquer chamada a `connectPrinter()` falha imediatamente na linha 163 com `"Dispositivo USB não definido"`, porque `usbDevice` é null. Isso afeta todos os fluxos que chamam `disconnect()` e depois tentam reconectar sem chamar `initializePrinter()` novamente.

Mais crítico: `initializePrinter()` (linha 42) chama `disconnect()` internamente quando o device muda — e logo em seguida define `usbDevice = device`. Mas `disconnect()` zera `usbDevice` antes que a nova atribuição aconteça. Em uso sequencial isso funciona, mas cria acoplamento frágil e confuso.

```kotlin
// disconnect() linha 205
} finally {
    connection = null
    usbDevice = null   // ← qualquer reconnect subsequente falha
}

// connectPrinter() linha 163–165
if (usbDevice == null) {
    Timber.e("Dispositivo USB não definido")
    return false       // ← falha silenciosa
}
```

### Passos para corrigir

1. **Remover `usbDevice = null` do `disconnect()`** — apenas `connection` precisa ser zerada:
   ```kotlin
   } finally {
       connection = null
       // NÃO zerar usbDevice aqui
   }
   ```

2. **Zerar `usbDevice` somente quando o device físico for desconectado** (receptor USB_DEVICE_DETACHED, linha 150–157), que já faz isso corretamente:
   ```kotlin
   UsbManager.ACTION_USB_DEVICE_DETACHED -> {
       val device = intent.parcelable<UsbDevice>(UsbManager.EXTRA_DEVICE)
       if (device == usbDevice) {
           disconnect()
           usbDevice = null   // correto aqui, pois device realmente saiu
       }
   }
   ```

---

## Issue #H2 — `onConnectionResult` sobrescrito em chamadas concorrentes

**Severidade:** ALTA
**Linhas:** 37, 54–75 (requestPermissionAndConnect), 177–181 (safeConnectSuspend)

### Descrição

`onConnectionResult` é um campo da classe. Se `safeConnectSuspend()` for chamado duas vezes antes do callback retornar (ex: usuário toca "Imprimir" duas vezes rapidamente), a segunda chamada sobrescreve o callback da primeira. A primeira coroutine fica suspensa indefinidamente, nunca retomada — **deadlock de coroutine**.

```kotlin
// linha 37 — campo único, não thread-safe
private var onConnectionResult: ((Boolean, String) -> Unit)? = null

// linha 56 — segunda chamada sobrescreve o callback anterior
fun requestPermissionAndConnect(device: UsbDevice, onResult: (Boolean, String) -> Unit) {
    usbDevice = device
    onConnectionResult = onResult   // ← sobrescreve callback anterior!
    // ...
}
```

### Passos para corrigir

1. **Adicionar um guard que rejeita nova solicitação enquanto outra está pendente:**
   ```kotlin
   private var connectionPending = false

   private fun safeConnectOrRequestPermission(onReady: (Boolean, String) -> Unit) {
       usbDevice?.let {
           if (connectionPending) {
               onReady(false, "Conexão já em andamento")
               return
           }
           if (usbManager.hasPermission(it)) {
               val connected = connectPrinter()
               onReady(connected, if (connected) "Conectado" else "Falha na conexão")
           } else {
               connectionPending = true
               requestPermissionAndConnect(it, { success, msg ->
                   connectionPending = false
                   onReady(success, msg)
               })
           }
       } ?: onReady(false, "Dispositivo USB não definido")
   }
   ```

2. **Limpar `onConnectionResult` após uso** no receiver (após linha 143):
   ```kotlin
   onConnectionResult = null
   ```

---

## Issue #H3 — `safeConnectSuspend()` não trata cancelamento da coroutine

**Severidade:** ALTA
**Linhas:** 177–181

### Descrição

`suspendCancellableCoroutine` é usado corretamente, mas o callback de cancelamento `{}` é vazio. Se a coroutine for cancelada (ex: ViewModel destruído) enquanto aguarda o resultado da permissão USB, o `onConnectionResult` ainda aponta para o `cont` cancelado. Quando o broadcast chegar, `cont.resume(success) {}` será chamado em uma continuation já cancelada, podendo lançar `IllegalStateException`.

```kotlin
suspend fun safeConnectSuspend(): Boolean = suspendCancellableCoroutine { cont ->
    safeConnectOrRequestPermission { success, _ ->
        cont.resume(success) {}   // ← onCancellation vazio; cont pode estar cancelado
    }
}
```

### Passos para corrigir

1. **Limpar o callback de resultado no bloco de cancelamento:**
   ```kotlin
   suspend fun safeConnectSuspend(): Boolean = suspendCancellableCoroutine { cont ->
       safeConnectOrRequestPermission { success, _ ->
           if (cont.isActive) {
               cont.resume(success) {}
           }
       }
       cont.invokeOnCancellation {
           onConnectionResult = null
       }
   }
   ```

---

## Issue #H4 — Interface USB incorreta liberada em `disconnect()`

**Severidade:** ALTA
**Linhas:** 196–199 (disconnect), 88–126 (printZpl)

### Descrição

`disconnect()` sempre libera `device.getInterface(0)` — a interface de índice 0. Porém `printZpl()` busca e reivindica **a primeira interface que contém um endpoint BULK OUT**, que não necessariamente é a de índice 0. O resultado:
- A interface realmente usada na impressão pode nunca ser liberada.
- `getInterface(0)` pode ser uma interface diferente (ex: HID), cuja liberação é inócua ou causa erro silencioso.

```kotlin
// disconnect() linha 196 — sempre libera interface[0]
usbDevice?.getInterface(0)?.let {
    connection?.releaseInterface(it)
}

// printZpl() linhas 88–98 — usa a primeira interface com endpoint BULK OUT
val usbInterface = usbDevice?.let { device ->
    (0 until device.interfaceCount)
        .map { device.getInterface(it) }
        .firstOrNull { iface -> /* endpoint BULK OUT */ }
}
// esta interface pode ser getInterface(1), getInterface(2), etc.
```

### Passos para corrigir

1. **Guardar referência à interface atualmente reivindicada como campo da classe:**
   ```kotlin
   private var activeInterface: UsbInterface? = null
   ```

2. **Atribuir em `printZpl()` ao reivindicar:**
   ```kotlin
   if (connection!!.claimInterface(usbInterface, true)) {
       activeInterface = usbInterface
       // ... bulkTransfer ...
   }
   ```

3. **Liberar a interface correta em `disconnect()`:**
   ```kotlin
   override fun disconnect() {
       try {
           activeInterface?.let { connection?.releaseInterface(it) }
           connection?.close()
       } catch (e: Exception) {
           Timber.e(e, "Erro ao desconectar da impressora")
       } finally {
           connection = null
           activeInterface = null
       }
   }
   ```

---

## Issue #H5 — Conexão não fechada se `buildZpl()` lançar exceção

**Severidade:** ALTA
**Linhas:** 210–227

### Descrição

Em `print()`, a conexão USB é estabelecida (linha 212) antes de `buildZpl()` ser chamado (linha 220). Se `buildZpl()` lançar uma exceção (ex: template não encontrado, `IOException`), a exceção é relançada na linha 223 sem que `disconnect()` seja chamado. A conexão USB fica aberta e não pode ser reaberta corretamente na próxima impressão.

```kotlin
fun print(product: MgvProduct, showQuantity: Boolean) {
    if (connection == null) {
        val connected = connectPrinter()   // abre conexão
        if (!connected || connection == null) return
    }

    val zpl = try {
        buildZpl(context, product, showQuantity)
    } catch (e: Exception) {
        throw e   // ← conexão não fechada; recurso vazado
    }
    printZpl(zpl)
}
```

### Passos para corrigir

1. **Remover o try/catch inútil** (que só relança) e **encapsular toda a operação em try/finally**:
   ```kotlin
   fun print(product: MgvProduct, showQuantity: Boolean) {
       if (connection == null) {
           val connected = connectPrinter()
           if (!connected || connection == null) {
               Timber.e("Conexão falhou ou nula após tentativa de conectar")
               return
           }
       }
       try {
           val zpl = buildZpl(context, product, showQuantity)
           Timber.d("ZPL gerado:\n$zpl")
           printZpl(zpl)
       } catch (e: Exception) {
           Timber.e(e, "Erro durante impressão")
           throw e
       } finally {
           disconnect()   // sempre libera, independente de sucesso ou falha
       }
   }
   ```

---

## Issue #H6 — Overflow de inteiro no cálculo da data de validade

**Severidade:** MÉDIA
**Linha:** 291

### Descrição

`diasValidade` é `Int`. A expressão `diasValidade * 24 * 60 * 60 * 1000` realiza aritmética em `Int`. Para `diasValidade >= 25`, o resultado ultrapassa `Int.MAX_VALUE` (~2,1 bilhões), causando overflow silencioso e datas de validade erradas.

```kotlin
// linha 291
val validadeMillis = dataAtualMillis + diasValidade * 24 * 60 * 60 * 1000
//                                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                                     Int arithmetic: overflow para diasValidade >= 25!
// Ex: 30 dias → 30 * 86400000 = 2.592.000.000 → overflow (Int.MAX = 2.147.483.647)
```

`dataAtualMillis` é `Long` (retorno de `System.currentTimeMillis()`), mas a multiplicação à direita do `+` é calculada em `Int` antes de ser somada.

### Passos para corrigir

1. **Forçar aritmética em `Long`** com sufixo `L` no literal ou `.toLong()`:**
   ```kotlin
   val validadeMillis = dataAtualMillis + diasValidade.toLong() * 24 * 60 * 60 * 1000L
   ```

---

## Issue #H7 — `BroadcastReceiver` pode vazar se broadcast nunca chegar

**Severidade:** MÉDIA
**Linhas:** 64–74 (registro), 144–147 (desregistro)

### Descrição

O `usbReceiver` é registrado em `requestPermissionAndConnect()` e **desregistrado apenas dentro do próprio receiver** (após processar o broadcast). Se por qualquer motivo o broadcast de permissão não chegar (ex: app foi para background, Activity destruída), o receiver permanece registrado indefinidamente, vazando o contexto.

```kotlin
// registrado em requestPermissionAndConnect()
context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

// desregistrado apenas dentro do próprio receiver — jamais no ciclo de vida externo
try { context.unregisterReceiver(this) } catch (_: Exception) { }
```

### Passos para corrigir

1. **Adicionar método `cleanup()` público** para ser chamado no `onDestroy()` ou `onCleared()` do ViewModel:
   ```kotlin
   fun cleanup() {
       try {
           context.unregisterReceiver(usbReceiver)
       } catch (_: Exception) { }
       onConnectionResult = null
       disconnect()
   }
   ```

2. **Chamar `honeywellPrinter.cleanup()` no `onCleared()` do `MainViewModel`**, junto com `epson.finalizePrinter()`.

---

## Issue #H8 — Try/catch inútil em `print()` (código morto)

**Severidade:** BAIXA
**Linhas:** 219–224

### Descrição

O bloco `try/catch` que envolve `buildZpl()` captura `Exception` apenas para relançá-la sem nenhum tratamento adicional. É código morto que adiciona ruído sem utilidade.

```kotlin
val zpl = try {
    buildZpl(context, product, showQuantity)
} catch (e: Exception) {
    throw e   // ← idêntico a não ter try/catch
}
```

### Passos para corrigir

1. **Remover o try/catch** (já coberto pela correção do Issue #H5):
   ```kotlin
   val zpl = buildZpl(context, product, showQuantity)
   ```

---

## Issue #H9 — `connection!!` com force-unwrap após checagem de null

**Severidade:** BAIXA
**Linhas:** 100, 118–125

### Descrição

`connection` é verificado como null na linha 100 e a função retorna se for null. Mas nas linhas 118 e 125, `connection!!` usa force-unwrap. Em código single-threaded isso é seguro, mas qualquer acesso concorrente (ex: `disconnect()` chamado de outro thread enquanto `printZpl()` executa) causaria `NullPointerException` no `!!`.

```kotlin
if (usbInterface == null || connection == null) return   // linha 100

// ...mais adiante...
if (connection!!.claimInterface(usbInterface, true)) {   // linha 118 — !!
    connection!!.bulkTransfer(...)                        // linha 119 — !!
    // ...
} finally {
    connection!!.releaseInterface(usbInterface)          // linha 125 — !!
}
```

### Passos para corrigir

1. **Capturar `connection` em variável local imutável** após a checagem:
   ```kotlin
   val conn = connection ?: return

   if (conn.claimInterface(usbInterface, true)) {
       val result = conn.bulkTransfer(endpointOut, bytes, bytes.size, 2000)
       Timber.d("Resultado da transferência: $result")
   } else {
       Timber.e("Falha ao reivindicar interface USB")
   }
   // finally
   conn.releaseInterface(usbInterface)
   ```

---

## Issue #H10 — `onConnectionResult` não limpo após invocação

**Severidade:** BAIXA
**Linhas:** 129–160 (usbReceiver)

### Descrição

Após `onConnectionResult?.invoke(...)` ser chamado no receiver (linhas 136–143), o campo nunca é zerado. Se o receiver disparar novamente (improvável para `ACTION_USB_PERMISSION`, mas possível via `ACTION_USB_DEVICE_DETACHED` encadeado), o callback obsoleto seria invocado novamente.

### Passos para corrigir

1. **Zerar `onConnectionResult` após invocar:**
   ```kotlin
   ACTION_USB_PERMISSION -> {
       val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
       val result = if (granted) connectPrinter() else false
       onConnectionResult?.invoke(result, if (result) "Conectado" else "Falha/Permissão negada")
       onConnectionResult = null   // ← limpar após uso
       try { context.unregisterReceiver(this) } catch (_: Exception) { }
   }
   ```

---

## Resumo HoneywellPrinter.kt

| # | Título | Severidade |
|---|--------|-----------|
| H1 | `disconnect()` zera `usbDevice`, quebrando reconexões | ALTA |
| H2 | `onConnectionResult` sobrescrito em chamadas concorrentes | ALTA |
| H3 | `safeConnectSuspend()` não trata cancelamento da coroutine | ALTA |
| H4 | Interface USB incorreta liberada em `disconnect()` | ALTA |
| H5 | Conexão não fechada se `buildZpl()` lançar exceção | ALTA |
| H6 | Overflow de inteiro no cálculo da data de validade | MÉDIA |
| H7 | `BroadcastReceiver` pode vazar se broadcast nunca chegar | MÉDIA |
| H8 | Try/catch inútil em `print()` | BAIXA |
| H9 | `connection!!` com force-unwrap após checagem de null | BAIXA |
| H10 | `onConnectionResult` não limpo após invocação | BAIXA |

---

## Prioridade de Resolução Sugerida

```
Fase 1 — Corretude crítica (resolver primeiro):
  #7   Lógica de permissão invertida               → 15 min
  #8   Spinner com objetos errados                  → 20 min
  #4   device.uuids[0] sem verificação              → 20 min
  #9   Rollback do estado de tara                   → 30 min
  #H1  disconnect() zera usbDevice                  → 15 min
  #H5  Conexão USB não fechada após exceção         → 20 min
  #H6  Overflow na data de validade                 → 5 min

Fase 2 — Estabilidade e recursos:
  #3   Socket BT não fechado no catch               → 45 min
  #10  Sockets BT não fechados no onCleared()       → 30 min
  #11  Busy-wait sem delay                          → 20 min
  #13  Flow continua após ViewModel destruído       → 30 min
  #H4  Interface USB incorreta liberada             → 20 min
  #H7  BroadcastReceiver pode vazar                 → 20 min

Fase 3 — Confiabilidade de protocolo e concorrência:
  #1   Canal de peso compartilhado                  → 60 min
  #6   Escritas concorrentes no socket              → depende do #2
  #2   Mutex nos streams do socket                  → 45 min
  #H2  onConnectionResult sobrescrito               → 30 min
  #H3  safeConnectSuspend sem cancelamento          → 20 min

Fase 4 — Qualidade e manutenibilidade:
  #5   Race condition no spinner                    → 20 min
  #12  Semântica da ClosedSocketException           → 30 min
  #14  IOException perdida no catch                 → 15 min
  #15  mapIndexed descartado                        → 5 min (junto com #8)
  #16  ConcurrentModificationException              → 10 min
  #H8  Try/catch inútil em print()                  → 5 min
  #H9  connection!! com force-unwrap                → 10 min
  #H10 onConnectionResult não limpo após invoke     → 5 min
```
