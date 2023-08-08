package piuk.blockchain.android.ui.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.blockchain.componentlib.viewextensions.gone
import com.jakewharton.rxbinding4.appcompat.queryTextChanges
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.SortedMap
import java.util.concurrent.TimeUnit
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSelectCountryBinding
import timber.log.Timber

class CountryDialog(
    context: Context,
    private val countryListSource: Single<SortedMap<String, String>>,
    private val listener: CountryCodeSelectionListener
) : Dialog(context) {

    private val compositeDisposable = CompositeDisposable()
    private val binding: DialogSelectCountryBinding =
        DialogSelectCountryBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        compositeDisposable +=
            countryListSource
                .subscribeBy(
                    onSuccess = { renderCountryMap(it) },
                    onError = {
                        Timber.e(it)
                        cancel()
                    }
                )
    }

    private fun renderCountryMap(countryMap: SortedMap<String, String>) {
        val arrayAdapter = ArrayAdapter<String>(
            context,
            android.R.layout.simple_list_item_1,
            android.R.id.text1,
            countryMap.keys.toTypedArray()
        )
        with(binding) {
            listViewCountries.adapter = arrayAdapter
            progressBarSelectCountryDialog.gone()

            listViewCountries.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val item = parent!!.getItemAtPosition(position).toString()
                    val code = countryMap[item]!!
                    listener.onCountrySelected(code, item)
                    dismiss()
                }

            searchViewCountry.apply {
                queryHint = context.getString(com.blockchain.stringResources.R.string.search_country)

                this.queryTextChanges()
                    .skipInitialValue()
                    .debounce(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .doOnNext { arrayAdapter.filter.filter(it) }
                    .subscribe()
            }
        }
    }

    override fun cancel() {
        super.cancel()
        compositeDisposable.clear()
    }

    interface CountryCodeSelectionListener {
        fun onCountrySelected(code: String, name: String)
    }
}
