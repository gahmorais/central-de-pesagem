package br.com.backupautomacao.centraldepesagem

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import br.com.backupautomacao.centraldepesagem.models.MgvProductImpl
import br.com.backupautomacao.centraldepesagem.ui.activities.MainActivity
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.net.NoRouteToHostException

@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {
  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
  private val application = ApplicationProvider.getApplicationContext<AppApplication>()
  private val viewModel = MainFragmentViewModel(application)
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

  @Test
  fun should_show_error_message_when_product_undefined() {
    viewModel.print()
    val messageExpected = application.getString(R.string.error_to_get_product)
    activityScenarioRule.scenario.onActivity {
      assertEquals(messageExpected, viewModel.statusMessage.value)
    }
  }

  @Test
  fun should_show_message_error_when_weighing_but_product_undefined() {
    viewModel.weighing()
    val messageExpected = application.getString(R.string.error_get_product)
    activityScenarioRule.scenario.onActivity {
      assertEquals(messageExpected, viewModel.statusMessage.value)
    }
  }

  @Test
  fun should_update_labelType_when_call_updateLabelType() {
    val expectedLabel = "ET313"
    viewModel.updateLabelType(expectedLabel)
    activityScenarioRule.scenario.onActivity { activity ->
      assertEquals(expectedLabel, viewModel.labelType.value)
    }
  }

  @Test
  fun should_show_error_message_when_code_is_empty() {
    val messageExpected = application.getString(R.string.barcode_error)
    viewModel.fetchItem("")
    activityScenarioRule.scenario.onActivity { activity ->
      assertEquals(messageExpected, viewModel.statusMessage.value)
    }
  }

  @Test
  fun should_get_product_when_call_fetchItem() {
    viewModel.fetchItem("1")
    activityScenarioRule.scenario.onActivity {
      assertTrue(productExpected == viewModel.mgvProductImpl.value)
    }
  }

}