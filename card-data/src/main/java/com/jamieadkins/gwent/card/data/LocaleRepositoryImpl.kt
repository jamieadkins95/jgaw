package com.jamieadkins.gwent.card.data

import android.content.res.Resources
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.jamieadkins.gwent.domain.LocaleRepository
import io.reactivex.Observable
import java.util.*
import javax.inject.Inject

class LocaleRepositoryImpl @Inject constructor(
    private val preferences: RxSharedPreferences,
    private val resources: Resources
) : LocaleRepository {

    override fun getLocale(): Observable<String> {
        val key = resources.getString(R.string.pref_locale_key)
        if (!preferences.getString(key).isSet) {
            val deviceLanguage = Locale.getDefault().language
            val cardLanguage = resources.getStringArray(R.array.locales).firstOrNull { it.contains(deviceLanguage) } ?: Constants.DEFAULT_LOCALE
            preferences.getString(key).set(cardLanguage)
        }
        return preferences.getString(key).asObservable()
    }

    override fun getNewsLocale(): Observable<String> {
        val key = resources.getString(R.string.pref_news_notifications_key)
        return preferences.getString(key, "en").asObservable()
    }

    override fun getDefaultLocale(): Observable<String> {
        return Observable.just(Constants.DEFAULT_LOCALE)
    }

}