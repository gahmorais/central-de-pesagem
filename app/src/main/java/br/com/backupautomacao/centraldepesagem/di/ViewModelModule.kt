package br.com.backupautomacao.centraldepesagem.di

import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainViewModel
import br.com.backupautomacao.centraldepesagem.ui.zpl.LabelViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.ReportViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
  viewModel {
    MainViewModel(
      get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
    )
  }
//  viewModel { LabelViewModel(get(), get(), get()) }
  viewModel { SettingsViewModel(get(), get()) }
  viewModel { ReportViewModel(get()) }
}