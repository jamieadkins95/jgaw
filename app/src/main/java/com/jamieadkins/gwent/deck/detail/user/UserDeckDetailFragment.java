package com.jamieadkins.gwent.deck.detail.user;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;

import com.jamieadkins.gwent.Injection;
import com.jamieadkins.gwent.R;
import com.jamieadkins.gwent.base.GwentRecyclerViewAdapter;
import com.jamieadkins.gwent.card.CardFilter;
import com.jamieadkins.gwent.data.CardDetails;
import com.jamieadkins.gwent.data.Deck;
import com.jamieadkins.gwent.data.Faction;
import com.jamieadkins.gwent.data.Filterable;
import com.jamieadkins.gwent.data.Type;
import com.jamieadkins.gwent.deck.detail.BaseDeckDetailFragment;

import java.util.List;

/**
 * UI fragment that shows a list of the users decks.
 */

public class UserDeckDetailFragment extends BaseDeckDetailFragment<UserDeckDetailsContract.View>
        implements UserDeckDetailsContract.View {

    protected UserDeckDetailsContract.Presenter deckDetailsPresenter;

    @Override
    public void onDeckUpdated(@NonNull Deck deck) {

    }

    @Override
    public void setupPresenter() {
        UserDeckDetailsPresenter presenter = new UserDeckDetailsPresenter(
                mDeckId,
                mFactionId,
                Injection.INSTANCE.provideDecksInteractor(getContext()),
                Injection.INSTANCE.provideCardsInteractor(getContext()));
        deckDetailsPresenter = presenter;
        setPresenter(presenter);
    }

    @Override
    public void showPotentialLeaders(List<CardDetails> potentialLeaders) {
        mPotentialLeaders = potentialLeaders;
        getActivity().invalidateOptionsMenu();
    }

    private List<CardDetails> mPotentialLeaders;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public static UserDeckDetailFragment newInstance(String deckId, String factionId) {
        UserDeckDetailFragment fragment = new UserDeckDetailFragment();
        fragment.mDeckId = deckId;
        fragment.mFactionId = factionId;
        return fragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_user_deck_detail;
    }

    @Override
    public GwentRecyclerViewAdapter onBuildRecyclerView() {
        return new GwentRecyclerViewAdapter.Builder()
                .withControls(GwentRecyclerViewAdapter.Controls.DECK)
                .build();
    }

    @Override
    public CardFilter initialiseCardFilter() {
        CardFilter filter = new CardFilter();
        filter.put(Type.LEADER_ID, false);
        filter.setCollectibleOnly(true);
        for (Filterable faction : Faction.ALL_FACTIONS) {
            if (!faction.getId().equals(mFactionId)) {
                filter.put(faction.getId(), false);
            }
        }
        filter.put(Faction.NEUTRAL_ID, true);
        filter.setCurrentFilterAsBase();
        return filter;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mPotentialLeaders != null && mPotentialLeaders.size() >= 3) {

            String key = getString(R.string.pref_locale_key);
            String locale = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getString(key, getString(R.string.default_locale));

            inflater.inflate(R.menu.deck_builder, menu);
            SubMenu subMenu = menu.findItem(R.id.action_change_leader).getSubMenu();
            for (final CardDetails leader : mPotentialLeaders) {
                subMenu.add(leader.getName(locale))
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                deckDetailsPresenter.changeLeader(leader.getIngameId());
                                return true;
                            }
                        });
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rename:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_edit_text, null);
                final EditText input = (EditText) view.findViewById(R.id.edit_text);
                input.setText(mDeck.getName());
                input.setHint(R.string.new_name);
                builder.setView(view)
                        .setTitle(R.string.rename)
                        .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deckDetailsPresenter.renameDeck(input.getText().toString());
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
                return true;
            case R.id.action_delete:
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.confirm_delete)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deckDetailsPresenter.deleteDeck();
                                getActivity().finish();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
