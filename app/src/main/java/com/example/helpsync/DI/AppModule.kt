package com.example.helpsync.DI

import com.example.helpsync.repository.CloudMessageRepository
import org.koin.dsl.module
import org.koin.compose.viewmodel.dsl.viewModel
import com.example.helpsync.repository.CloudMessageRepositoryImpl
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel

val appModule = module{
    single<CloudMessageRepository> { CloudMessageRepositoryImpl() }

    viewModel { HelpMarkHolderViewModel(get()) }
    viewModel { UserViewModel() }
}