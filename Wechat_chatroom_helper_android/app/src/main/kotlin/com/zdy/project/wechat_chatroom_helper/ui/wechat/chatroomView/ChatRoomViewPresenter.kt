package com.zdy.project.wechat_chatroom_helper.ui.wechat.chatroomView

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.*

/**
 * Created by Mr.Zdy on 2017/8/25.
 */

class ChatRoomViewPresenter(mContext: Context, pageType: Int) : ChatRoomContract.Presenter {

    override val presenterView: ViewGroup
    override lateinit var originAdapter: Any

    val isShowing: Boolean get() = mView.isShowing


    private val mView: ChatRoomContract.View


    init {
        presenterView = FrameLayout(mContext).also {
            it.layoutParams = ViewGroup.LayoutParams(ViewGroup
                    .LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        mView = ChatRoomView(mContext, presenterView, pageType)
        mView.setPresenter(this)
    }


    fun setOnDialogItemClickListener(listener: ChatRoomRecyclerViewAdapter.OnDialogItemClickListener) {
        mView.setOnDialogItemClickListener(listener)
    }

    fun setAdapter(mAdapter: Any) {
        this.originAdapter = mAdapter
    }

    fun setListInAdapterPositions(listInAdapterPositions: ArrayList<Int>) {
        mView.showMessageRefresh(listInAdapterPositions)
    }

    override fun setMessageRefresh(targetUserName: String) {
        mView.showMessageRefresh(targetUserName)
    }

    override fun show() {
        mView.show()
    }

    override fun dismiss() {
        mView.dismiss()
    }

    override fun start() {
        mView.init()
    }


}