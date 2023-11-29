package br.com.backupautomacao.centraldepesagem.network

import androidx.test.core.app.ApplicationProvider
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.errors.MgvProductNotFoundedException
import br.com.backupautomacao.centraldepesagem.errors.MgvStoreNotFoundedException
import br.com.backupautomacao.centraldepesagem.models.MgvProductImpl
import io.ktor.util.reflect.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.net.ConnectException
import java.net.NoRouteToHostException

private const val KEYWORD = "bkp@2018"
private const val STORE = "1"
private const val CODE_PRODUCT = "1"

class MgvRepositoryTest {
  private val mgvRepository = MgvRepository()
  private val productExpected = MgvProductImpl(
    nutrictionFacts = null,
    extraInfo = null,
    code = "1",
    description = "BACON MANTA RIO SUL kg",
    codExtraInfo = null,
    codNutrictionFacts = null,
    codTara = null,
    shelfLife = 0,
    price = 222.00,
    batch = "008376245920",
    promotionalPrice = 0.0,
    totalPrice = null,
    unit = null,
    weight = null
  )

  private fun setup(serverAddress: String) {
    val context = ApplicationProvider.getApplicationContext<AppApplication>()
    val configRepository = ConfigRepository(context)
    configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, serverAddress)
  }

  @Test
  fun should_throw_connect_exception_when_server_undefined() {
    setup("")
    assertThrows(ConnectException::class.java) {
      runBlocking { mgvRepository.queryItem(KEYWORD, STORE, CODE_PRODUCT) }
    }
  }

  @Test
  fun should_throw_store_not_found_exception_when_store_incorrect() {
    setup("http://172.16.0.14:9300/MGV6_WCF/REST/")
    val invalidStore = "2"
    assertThrows(MgvStoreNotFoundedException::class.java) {
      runBlocking { mgvRepository.queryItem(KEYWORD, invalidStore, CODE_PRODUCT) }
    }
  }

  @Test
  fun should_throw_prouct_not_found_exception_when_product_not_exists() {
    setup("http://172.16.0.14:9300/MGV6_WCF/REST/")
    val invalidProduct = "123456789"
    assertThrows(MgvProductNotFoundedException::class.java) {
      runBlocking { mgvRepository.queryItem(KEYWORD, STORE, invalidProduct) }
    }
  }

  @Test
  fun should_return_product_when_code_exists() {
    setup("http://172.16.0.14:9300/MGV6_WCF/REST/")
    val validProduct = "1"
    val product = runBlocking { mgvRepository.queryItem(KEYWORD, STORE, validProduct) }
    assertEquals(productExpected, product)
  }

  @Test
  fun should_return_nutriction_info_when_code_exists() {
    setup("http://172.16.0.14:9300/MGV6_WCF/REST/")
    val validProductWithNutriInfo = "896"
    val product = runBlocking { mgvRepository.queryItem(KEYWORD, STORE, validProductWithNutriInfo) }
    val nutriction = runBlocking {
      mgvRepository.getNutriction(
        KEYWORD,
        STORE,
        product?.codNutrictionFacts!!
      )
    }
    assertTrue(nutriction != null)
  }

  @Test
  fun should_return_extra_info_when_code_exists() {
    setup("http://172.16.0.14:9300/MGV6_WCF/REST/")
    val validProductWithExtraInfo = "896"
    val product = runBlocking { mgvRepository.queryItem(KEYWORD, STORE, validProductWithExtraInfo) }
    val extraInfo =
      runBlocking { mgvRepository.getExtraInfo(KEYWORD, STORE, product?.codExtraInfo!!) }
    assertTrue(extraInfo != null)
  }

}