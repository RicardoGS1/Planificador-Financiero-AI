package com.virtualworld.easyexpensecontrol.di

import androidx.room.Room
import com.virtualworld.easyexpensecontrol.BuildConfig
import com.virtualworld.easyexpensecontrol.data.local.BudgetListVisibilityRepository
import com.virtualworld.easyexpensecontrol.data.local.FinancialDatabase
import com.virtualworld.easyexpensecontrol.data.remote.GeminiApi
import com.virtualworld.easyexpensecontrol.data.remote.ReceiptRemoteDataSource
import com.virtualworld.easyexpensecontrol.data.repository.BudgetRepository
import com.virtualworld.easyexpensecontrol.data.repository.CategoryRepository
import com.virtualworld.easyexpensecontrol.data.repository.ReceiptAnalysisRepositoryImpl
import com.virtualworld.easyexpensecontrol.data.repository.TransactionRepository
import com.virtualworld.easyexpensecontrol.domain.repository.BudgetRepository as BudgetRepositoryDomain
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository as CategoryRepositoryDomain
import com.virtualworld.easyexpensecontrol.domain.repository.ReceiptAnalysisRepository
import com.virtualworld.easyexpensecontrol.domain.repository.TransactionRepository as TransactionRepositoryDomain
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.AddBudgetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.DeleteBudgetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetForCategoryMonthAndYearUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.UpdateBudgetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesByTypeUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByNameUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessReceiptUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.DeleteTransactionUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsByCategoryAndDateUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.SaveTransactionUseCase
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            FinancialDatabase::class.java,
            "financialapp.db"
        ).build()
    }

    single { get<FinancialDatabase>().transactionDao() }
    single { get<FinancialDatabase>().categoryDao() }
    single { get<FinancialDatabase>().budgetDao() }

    single { BudgetListVisibilityRepository(androidContext()) }

    // Repositorios (implementaciones data que cumplen interfaces domain)
    single<TransactionRepositoryDomain> { TransactionRepository(get()) }
    single<CategoryRepositoryDomain> { CategoryRepository(get()) }
    single<BudgetRepositoryDomain> { BudgetRepository(get()) }

    // Casos de uso - Transaction
    single { GetTransactionsUseCase(get()) }
    single { GetTransactionByIdUseCase(get()) }
    single { GetTransactionsByCategoryAndDateUseCase(get()) }
    single { SaveTransactionUseCase(get(), get(), get()) }
    single { DeleteTransactionUseCase(get(), get()) }

    // Casos de uso - Category
    single { GetCategoriesUseCase(get()) }
    single { GetCategoryByIdUseCase(get()) }
    single { GetCategoryByNameUseCase(get()) }
    single { GetCategoriesByTypeUseCase(get()) }

    // Red - Gemini / análisis de comprobantes
    single { OkHttpClient.Builder().build() }
    single { Gson() }
    single {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>().create(GeminiApi::class.java) }
    single { ReceiptRemoteDataSource(get(), BuildConfig.GEMINI_API_KEY, get(), androidContext()) }
    single<ReceiptAnalysisRepository> { ReceiptAnalysisRepositoryImpl(get()) }
    single { ProcessReceiptUseCase(get(), get()) }

    // Casos de uso - Budget
    single { GetBudgetsUseCase(get()) }
    single { GetBudgetByIdUseCase(get()) }
    single { GetBudgetForCategoryMonthAndYearUseCase(get()) }
    single { AddBudgetUseCase(get()) }
    single { UpdateBudgetUseCase(get()) }
    single { DeleteBudgetUseCase(get()) }

    // ViewModels
    viewModel {
        TransactionViewModel(
            getTransactionsUseCase = get(),
            getTransactionByIdUseCase = get(),
            getTransactionsByCategoryAndDateUseCase = get(),
            saveTransactionUseCase = get(),
            deleteTransactionUseCase = get(),
            processReceiptUseCase = get(),
            getCategoryByNameUseCase = get(),
            appContext = androidContext()
        )
    }
    viewModel {
        BudgetViewModel(
            getBudgetsUseCase = get(),
            getBudgetByIdUseCase = get(),
            getBudgetForCategoryMonthAndYearUseCase = get(),
            addBudgetUseCase = get(),
            updateBudgetUseCase = get(),
            deleteBudgetUseCase = get()
        )
    }
    viewModel {
        CategoryViewModel(
            getCategoriesUseCase = get(),
            getCategoryByIdUseCase = get(),
            getCategoryByNameUseCase = get(),
            getCategoriesByTypeUseCase = get()
        )
    }
}
