package com.jamieadkins.gwent.update

import com.jamieadkins.gwent.main.MvpPresenter

interface UpdateContract {

    interface View {

        fun openCardDatabase()
    }

    interface Presenter : MvpPresenter
}
