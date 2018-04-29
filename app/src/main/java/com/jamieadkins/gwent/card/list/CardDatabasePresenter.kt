package com.jamieadkins.gwent.card.list

import com.jamieadkins.commonutils.bus.RxBus
import com.jamieadkins.commonutils.mvp2.BasePresenter
import com.jamieadkins.commonutils.mvp2.BaseSchedulerProvider
import com.jamieadkins.commonutils.mvp2.addToComposite
import com.jamieadkins.gwent.base.BaseDisposableObserver
import com.jamieadkins.gwent.base.BaseDisposableSingle
import com.jamieadkins.gwent.bus.*
import com.jamieadkins.gwent.core.CardDatabaseResult
import com.jamieadkins.gwent.data.repository.card.CardRepository
import com.jamieadkins.gwent.data.repository.filter.FilterRepository
import com.jamieadkins.gwent.data.repository.update.UpdateRepository
import com.jamieadkins.gwent.view.bus.GwentCardClickEvent

class CardDatabasePresenter(schedulerProvider: BaseSchedulerProvider,
                            val cardRepository: CardRepository,
                            val updateRepository: UpdateRepository,
                            val filterRepository: FilterRepository) :
        BasePresenter<CardDatabaseContract.View>(schedulerProvider), CardDatabaseContract.Presenter {

    init {
        filterRepository.setDefaultFilters()
    }

    override fun onAttach(newView: CardDatabaseContract.View) {
        super.onAttach(newView)

        RxBus.register(RefreshEvent::class.java)
                .subscribeWith(object : BaseDisposableObserver<RefreshEvent>() {
                    override fun onNext(t: RefreshEvent) {
                        onRefresh()
                    }
                })
                .addToComposite(disposable)

        RxBus.register(ResetFiltersEvent::class.java)
                .subscribeWith(object : BaseDisposableObserver<ResetFiltersEvent>() {
                    override fun onNext(event: ResetFiltersEvent) {
                        filterRepository.resetFilters()
                    }
                })
                .addToComposite(disposable)

        RxBus.register(ScrollToTopEvent::class.java)
                .subscribeWith(object : BaseDisposableObserver<ScrollToTopEvent>() {
                    override fun onNext(t: ScrollToTopEvent) {
                        view?.scrollToTop()
                    }
                })
                .addToComposite(disposable)

        RxBus.register(GwentCardClickEvent::class.java)
                .subscribeWith(object : BaseDisposableObserver<GwentCardClickEvent>() {
                    override fun onNext(event: GwentCardClickEvent) {
                        view?.showCardDetails(event.data)
                    }
                })
                .addToComposite(disposable)

        filterRepository.getFilter()
                .observeOn(schedulerProvider.ui())
                .doOnNext { view?.setLoadingIndicator(true) }
                .observeOn(schedulerProvider.io())
                .switchMapSingle { cardRepository.getCards(it) }
                .observeOn(schedulerProvider.ui())
                .subscribeWith(object : BaseDisposableObserver<CardDatabaseResult>() {
                    override fun onNext(result: CardDatabaseResult) {
                        view?.showCards(result)
                        view?.setLoadingIndicator(false)
                    }
                })
                .addToComposite(disposable)
    }

    override fun onRefresh() {
        updateRepository.isUpdateAvailable()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(object : BaseDisposableSingle<Boolean>() {
                    override fun onSuccess(update: Boolean) {
                        if (update) {
                            view?.showUpdateAvailable()
                        }
                        view?.setLoadingIndicator(false)
                    }
                })
                .addToComposite(disposable)
    }

    override fun search(query: String) {
        filterRepository.updateSearchQuery(query)
    }

    override fun clearSearch() {
        filterRepository.clearSearchQuery()
    }
}
