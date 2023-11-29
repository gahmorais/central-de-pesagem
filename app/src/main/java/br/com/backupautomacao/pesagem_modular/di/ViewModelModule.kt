package br.com.backupautomacao.pesagem_modular.di

import br.com.backupautomacao.pesagem_modular.ui.viewmodel.MainFragmentViewModel
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.ReportViewModel
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.SaleForUnitViewModel
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
  viewModel {
    MainFragmentViewModel(
      configRepository = get(),
      operationRepository = get(),
      workManager = get(),
      bluetoothController = get(),
      labelBuild = get(),
      stringResourceProvider = get(),
      mgvRepository = get()
    )
  }

  viewModel {
    SaleForUnitViewModel(
      configRepository = get(),
      workManager = get(),
      labelBuilder = get()
    )
  }

  viewModel {
    SettingsViewModel(
      configRepository = get(),
      stringResourceProvider = get()
    )
  }

  viewModel {
    ReportViewModel(
      operationRepository = get()
    )
  }
}