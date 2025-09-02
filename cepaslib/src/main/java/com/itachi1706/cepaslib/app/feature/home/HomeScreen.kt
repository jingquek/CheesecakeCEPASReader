/*
 * HomeScreen.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.itachi1706.cepaslib.app.feature.home

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.TagLostException
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import autodispose2.autoDispose
import com.itachi1706.cepaslib.CEPASLibBuilder
import com.itachi1706.cepaslib.R
import com.itachi1706.cepaslib.SettingsHandler
import com.itachi1706.cepaslib.app.core.activity.ActivityOperations
import com.itachi1706.cepaslib.app.core.inject.ScreenScope
import com.itachi1706.cepaslib.app.core.ui.ActionBarOptions
import com.itachi1706.cepaslib.app.core.ui.FareBotScreen
import com.itachi1706.cepaslib.app.core.util.ActionBarOptionsDefaults
import com.itachi1706.cepaslib.app.core.util.ErrorUtils
import com.itachi1706.cepaslib.app.feature.card.CardScreen
import com.itachi1706.cepaslib.app.feature.help.HelpScreen
import com.itachi1706.cepaslib.app.feature.history.HistoryScreen
import com.itachi1706.cepaslib.app.feature.main.MainActivity.MainActivityComponent
import com.itachi1706.cepaslib.card.RawCard
import dagger.Component
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class HomeScreen : FareBotScreen<HomeScreen.HomeComponent, HomeScreenView>(),
    HomeScreenView.Listener {

    @Inject
    lateinit var activityOperations: ActivityOperations
    @Inject
    lateinit var cardStream: CardStream

    override fun onCreateView(context: Context): HomeScreenView = HomeScreenView(context, this)

    override fun getTitle(context: Context): String =
        CEPASLibBuilder.customTitle ?: context.getString(R.string.app_name)

    override fun getActionBarOptions(): ActionBarOptions =
        ActionBarOptionsDefaults.getActionBarOptionsDefault(
            backgroundColor = android.R.color.transparent,
            textColorRes = if (ActionBarOptionsDefaults.isNightModeEnabled(activity)) R.color.white else R.color.black,
            shadow = false
        )

    override fun onShow(context: Context) {
        super.onShow(context)

        activityOperations.menuItemClick
            .autoDispose(this)
            .subscribe { menuItem ->
                when (menuItem.itemId) {
                    R.id.history -> navigator.goTo(HistoryScreen())
                    R.id.help -> navigator.goTo(HelpScreen())
                    R.id.prefs -> activity.startActivity(SettingsHandler.launchSettings(activity))
                    else -> CEPASLibBuilder.processMenuItemsFurther(menuItem, activity)
                }
            }

        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (adapter == null) {
            view.showNfcError(HomeScreenView.NfcError.UNAVAILABLE)
        } else if (!adapter.isEnabled) {
            view.showNfcError(HomeScreenView.NfcError.DISABLED)
        } else {
            view.showNfcError(HomeScreenView.NfcError.NONE)
        }

        cardStream.observeCards()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this)
            .subscribe { card ->
                // Show tag ID conversion information
                showTagIdConversionInfo(card)
                navigator.goTo(CardScreen(card))
            }

        cardStream.observeLoading()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this)
            .subscribe { loading -> view.showLoading(loading) }

        cardStream.observeErrors()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this)
            .subscribe { ex ->
                when (ex) {
                    is CardStream.CardUnauthorizedException -> AlertDialog.Builder(activity)
                        .setTitle(R.string.locked_card)
                        .setMessage(R.string.keys_required)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()

                    is TagLostException -> AlertDialog.Builder(activity)
                        .setTitle(R.string.tag_lost)
                        .setMessage(R.string.tag_lost_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()

                    else -> {
                        ErrorUtils.showErrorAlert(activity, ex)
                    }
                }
            }
    }

    override fun onUpdateMenu(menu: Menu) {
        activity.menuInflater.inflate(R.menu.screen_main, menu)
        if (!CEPASLibBuilder.showAbout) menu.removeItem(R.id.about)
    }

    override fun onNfcErrorButtonClicked() {
        activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }

    private fun showTagIdConversionInfo(card: RawCard<*>) {
        val tagId = card.tagId()
        val rawBytes = tagId.bytes()
        val hexResult = tagId.hex()
        
        // Create a detailed conversion message
        val message = StringBuilder()
        message.append("Tag ID Conversion:\n")
        message.append("Raw: ${rawBytes.size} bytes\n")
        message.append("Hex: $hexResult\n")
        message.append("(${hexResult.length} hex characters)")
        
        // Show as toast
        Toast.makeText(activity, message.toString(), Toast.LENGTH_LONG).show()
        
        // Also log for debugging
        Log.d("HomeScreen", "Tag ID Conversion: ${rawBytes.size} bytes → $hexResult")
    }

    override fun createComponent(parentComponent: MainActivityComponent): HomeComponent =
        DaggerHomeScreen_HomeComponent.builder()
            .mainActivityComponent(parentComponent)
            .build()

    override fun inject(component: HomeComponent) {
        component.inject(this)
    }

    @ScreenScope
    @Component(dependencies = [MainActivityComponent::class])
    interface HomeComponent {
        fun inject(homeScreen: HomeScreen)
    }
}
