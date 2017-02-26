package com.jamieadkins.gwent.deck.list;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jamieadkins.commonutils.ui.Header;
import com.jamieadkins.gwent.R;
import com.jamieadkins.gwent.base.BaseFragment;
import com.jamieadkins.gwent.data.CardDetails;
import com.jamieadkins.gwent.data.Deck;
import com.jamieadkins.gwent.data.interactor.RxDatabaseEvent;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * UI fragment that shows a list of the users decks.
 */

public class DeckListFragment extends BaseFragment<Deck> implements DecksContract.View,
        NewDeckDialog.NewDeckDialogListener {
    private static final int REQUEST_CODE = 3414;
    private static final String STATE_PUBLIC_DECKS = "com.jamieadkins.gwent.user.decks";
    private DecksContract.Presenter mDecksPresenter;

    // Set up to show user decks by default.
    private boolean mPublicDecks = false;

    public DeckListFragment() {
    }

    public static DeckListFragment newInstance(boolean userDecks) {
        DeckListFragment fragment = new DeckListFragment();
        fragment.mPublicDecks = userDecks;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DeckRecyclerViewAdapter adapter = new DeckRecyclerViewAdapter();
        setRecyclerViewAdapter(adapter);

        if (savedInstanceState != null) {
            mPublicDecks = savedInstanceState.getBoolean(STATE_PUBLIC_DECKS);
        }

        if (!mPublicDecks) {
            getActivity().setTitle(getString(R.string.my_decks));
        } else {
            getActivity().setTitle(getString(R.string.public_decks));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_deck_list, container, false);

        setupViews(rootView);

        FloatingActionButton buttonNewDeck =
                (FloatingActionButton) rootView.findViewById(R.id.new_deck);

        if (!mPublicDecks) {
            buttonNewDeck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DialogFragment newFragment =  NewDeckDialog.newInstance(mDecksPresenter);
                    newFragment.setTargetFragment(DeckListFragment.this, REQUEST_CODE);
                    newFragment.show(getActivity().getSupportFragmentManager(),
                            newFragment.getClass().getSimpleName());
                }
            });
        } else {
            buttonNewDeck.setVisibility(View.GONE);
            buttonNewDeck.setEnabled(false);
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        onLoadData();
    }

    @Override
    public void onLoadData() {
        super.onLoadData();
        Observable<RxDatabaseEvent<Deck>> decks;
        if (!mPublicDecks) {
            decks = mDecksPresenter.getUserDecks();
        } else {
            decks = mDecksPresenter.getPublicDecks();
        }
        decks.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());

        if (mPublicDecks) {
            mDecksPresenter.getDeckOfTheWeek()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<RxDatabaseEvent<Deck>>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(RxDatabaseEvent<Deck> value) {
                            getRecyclerViewAdapter().addItem(0, new Header("Deck of the Week", "Week 1"));
                            getRecyclerViewAdapter().addItem(1, value.getValue());
                            getRecyclerViewAdapter().addItem(2, new Header("Featured Decks", "Highest Rated"));
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mDecksPresenter.stop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_PUBLIC_DECKS, mPublicDecks);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setPresenter(DecksContract.Presenter presenter) {
        mDecksPresenter = presenter;
    }

    @Override
    public void createNewDeck(String name, String faction, CardDetails leader) {
        mDecksPresenter.createNewDeck(name, faction, leader);
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        setLoading(active);
    }
}
