package com.jamieadkins.decktracker.data

import com.jamieadkins.gwent.domain.GwentFaction
import com.jamieadkins.gwent.domain.card.repository.CardRepository
import com.jamieadkins.gwent.domain.tracker.predictions.CardPrediction
import com.jamieadkins.gwent.domain.tracker.predictions.CardPredictions
import com.jamieadkins.gwent.domain.tracker.predictions.CardPredictorRepository
import io.reactivex.Single
import javax.inject.Inject

class CardPredictorRepositoryImpl @Inject constructor(
    private val api: CardPredictorApi,
    private val cardRepository: CardRepository
) : CardPredictorRepository {

    override fun getPredictions(faction: GwentFaction, leaderId: Long, cardIds: List<Long>): Single<CardPredictions> {
        return api.analyseDeck(mapFactionToString(faction), leaderId.toString(), cardIds.joinToString(","))
            .flatMap { response ->
                cardRepository.getCards(response.probabilities?.keys?.map { it.toString() } ?: emptyList())
                    .first(emptyList())
                    .map { cards ->
                        val predictions = response.probabilities?.entries?.mapNotNull { entry ->
                           cards.find { it.id == entry.key.toString() }?.let { card ->
                               CardPrediction(card, (entry.value * 100).toInt())
                           }
                        } ?: emptyList()
                        CardPredictions(response.similarDecks ?: 0, predictions)
                    }
            }
    }

    private fun mapFactionToString(faction: GwentFaction): String {
        return when (faction) {
            GwentFaction.MONSTER -> "monsters"
            GwentFaction.NORTHERN_REALMS -> "northernrealms"
            GwentFaction.SCOIATAEL -> "scoiatael"
            GwentFaction.SKELLIGE -> "skellige"
            GwentFaction.NILFGAARD -> "nilfgaard"
            GwentFaction.SYNDICATE -> "syndicate"
            else -> throw IllegalArgumentException("Unrecognised faction $faction")
        }
    }

    private fun mapStringToFaction(faction: String): GwentFaction {
        return when (faction) {
            "monsters" -> GwentFaction.MONSTER
            "northernrealms" -> GwentFaction.NORTHERN_REALMS
            "scoiatael" -> GwentFaction.SCOIATAEL
            "skellige" -> GwentFaction.SKELLIGE
            "nilfgaard" -> GwentFaction.NILFGAARD
            "syndicate" -> GwentFaction.SYNDICATE
            else -> throw IllegalArgumentException("Unrecognised faction $faction")
        }
    }
}