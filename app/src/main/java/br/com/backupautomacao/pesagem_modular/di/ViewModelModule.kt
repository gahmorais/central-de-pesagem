package br.com.backupautomacao.pesagem_modular.di

import br.com.backupautomacao.pesagem_modular.ui.viewmodel.MainViewModel
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.ReportViewModel
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.SaleForUnitViewModel
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
  viewModel {
    MainViewModel(
      configRepository = get(),
      operationRepository = get(),
      workManager = get(),
      btHandler = get(),
      labelBuild = get(),
      stringResourceProvider = get(),
      apiMgv = get()
    )
  }

  viewModel {
    SaleForUnitViewModel(
      configRepository = get(),
      workManager = get(),
      labelBuilder = get(),
      apiMgv = get()
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