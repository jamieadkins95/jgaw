package com.jamieadkins.gwent.deck.detail

import com.jamieadkins.gwent.domain.card.model.GwentCard
import com.jamieadkins.gwent.domain.deck.model.GwentDeck
import com.jamieadkins.gwent.main.MvpPresenter

interface DeckDetailsContract {

    interface View {

        fun showDeck(deck: GwentDeck)

        fun showCardDatabase(cards: List<GwentCard>, searchQuery: String)

        fun showLoadingIndicator()

        fun hideLoadingIndicator()

        fun showLeaderPicker()

        fun showRenameDeckMenu()

        fun close()

        fun showCardDetails(cardId: String)
    }

    interface Presenter : MvpPresenter {

        fun setDeckId(deckId: String)

        fun onChangeLeaderClicked()

        fun onRenameClicked()

        fun onDeleteClicked()

        fun search(query: String)

        fun clearSearch()
    }
}
