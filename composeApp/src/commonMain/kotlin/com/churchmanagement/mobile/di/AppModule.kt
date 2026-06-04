package com.churchmanagement.mobile.di

import com.churchmanagement.mobile.data.AuthRepository
import com.churchmanagement.mobile.data.BlogRepository
import com.churchmanagement.mobile.data.DevotionalRepository
import com.churchmanagement.mobile.data.EventRepository
import com.churchmanagement.mobile.data.ForumRepository
import com.churchmanagement.mobile.data.LeaderRepository
import com.churchmanagement.mobile.data.LiveStreamRepository
import com.churchmanagement.mobile.data.MemberRepository
import com.churchmanagement.mobile.data.NotificationRepository
import com.churchmanagement.mobile.data.PrayerRepository
import com.churchmanagement.mobile.data.ProjectRepository
import com.churchmanagement.mobile.data.SettingsRepository
import com.churchmanagement.mobile.data.UserRepository
import com.churchmanagement.mobile.sdui.data.AppConfigRepository
import com.churchmanagement.mobile.sdui.data.DataResolver
import com.churchmanagement.mobile.sdui.data.LayoutRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.dsl.module

val appModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }

    single { AuthRepository(get()) }
    single { EventRepository(get()) }
    single { DevotionalRepository(get()) }
    single { NotificationRepository(get()) }
    single { BlogRepository(get()) }
    single { ForumRepository(get()) }
    single { ProjectRepository(get()) }
    single { LiveStreamRepository(get()) }
    single { LeaderRepository(get()) }
    single { MemberRepository(get()) }
    single { PrayerRepository(get()) }
    single { SettingsRepository(get()) }
    single { UserRepository(get()) }
    single { LayoutRepository(get()) }
    single { AppConfigRepository(get()) }
    single { DataResolver(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
