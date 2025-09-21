package br.com.backupautomacao.pesagem_modular.di

import br.com.backupautomacao.pesagem_modular.ui.viewmodel.MainViewModel
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.ReportViewModel
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
  viewModel {
    MainViewModel(
      get(), get(), get(), get(), get(), get(), get(), get(), get()
    )
  }
  viewModel { SettingsViewModel(get(), get()) }
  viewModel { ReportViewModel(get()) }
}