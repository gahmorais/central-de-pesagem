package br.com.backupautomacao.centraldepesagem

import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.network.MgvRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private const val KEYWORD = "bkp@2018"
private const val CODE = "1"
private const val STORE_NUMBER = "1"

class MgvRepositoryTest {
  @Test
  fun test(){
    assertEquals(4,2+2)
  }
  private val mgvRepository = MgvRepository()

  @Test
  fun should_return_MgvResponseProduct_when_code_exists() = runBlocking {
    val response = mgvRepository.queryItem(KEYWORD, STORE_NUMBER, CODE)
    val expectedProduct = MgvProduct(
      null,
      null,
      "1",
      "BACON MANTA RIO SUL kg",
      null,
      null,
      null,
      0,
      222.0,
      "008376245920",
      0.0,
      0.0,
      0.0
    )
    assertEquals(expectedProduct, response)
  }

  fun should_throwError_when_code_not_exists() = runBlocking {
    val response = mgvRepository.queryItem(KEYWORD, STORE_NUMBER, "123456")
  }
}