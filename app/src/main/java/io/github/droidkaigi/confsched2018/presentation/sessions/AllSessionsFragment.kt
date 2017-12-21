package io.github.droidkaigi.confsched2018.presentation.sessions

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.transition.TransitionInflater
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.github.droidkaigi.confsched2018.R
import io.github.droidkaigi.confsched2018.databinding.FragmentAllSessionsBinding
import io.github.droidkaigi.confsched2018.di.Injectable
import io.github.droidkaigi.confsched2018.model.Session
import io.github.droidkaigi.confsched2018.presentation.Result
import io.github.droidkaigi.confsched2018.presentation.common.binding.FragmentDataBindingComponent
import io.github.droidkaigi.confsched2018.presentation.sessions.item.DateSessionsGroup
import io.github.droidkaigi.confsched2018.util.ext.addOnScrollListener
import io.github.droidkaigi.confsched2018.util.ext.isGone
import io.github.droidkaigi.confsched2018.util.ext.observe
import io.github.droidkaigi.confsched2018.util.ext.setTextIfChanged
import timber.log.Timber
import javax.inject.Inject

class AllSessionsFragment : Fragment(), Injectable {

    private lateinit var binding: FragmentAllSessionsBinding

    private val dataBindingComponent = FragmentDataBindingComponent(this)
    private val sessionsGroup = DateSessionsGroup(dataBindingComponent)

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private val sessionsViewModel: AllSessionsViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(AllSessionsViewModel::class.java)
    }

    private val dayVisibleConstraintSet by lazy {
        ConstraintSet().apply {
            clone(context, R.layout.fragment_all_sessions)
            setVisibility(R.id.day_header, ConstraintSet.VISIBLE)
        }
    }

    private val dayGoneConstraintSet by lazy {
        ConstraintSet().apply {
            clone(context, R.layout.fragment_all_sessions)
            setVisibility(R.id.day_header, ConstraintSet.GONE)
        }
    }

    private val onFavoriteClickListener = { session: Session ->
        // Just for response
        session.isFavorited = !session.isFavorited
        binding.sessionsRecycler.adapter.notifyDataSetChanged()

        sessionsViewModel.onFavoriteClick(session)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentAllSessionsBinding.inflate(inflater, container, false, dataBindingComponent)
        lifecycle.addObserver(sessionsViewModel)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        lifecycle.addObserver(sessionsViewModel)

        setupRecyclerView()

        sessionsViewModel.sessions.observe(this, { result ->
            when (result) {
                is Result.Success -> {
                    val sessions = result.data
                    sessionsGroup.updateSessions(sessions, onFavoriteClickListener)
                }
                is Result.Failure -> {
                    Timber.e(result.e)
                }
            }
        })
    }

    private fun setupRecyclerView() {
        val groupAdapter = GroupAdapter<ViewHolder>().apply {
            add(sessionsGroup)
            setOnItemClickListener({ item, view ->
                //TODO
            })
        }
        binding.sessionsRecycler.apply {
            adapter = groupAdapter

            addOnScrollListener(
                    onScrollStateChanged = { _: RecyclerView?, newState: Int ->
                        if (binding.sessionsRecycler.isGone()) return@addOnScrollListener
                        setDayHeaderVisibility(newState != RecyclerView.SCROLL_STATE_IDLE)
                    },
                    onScrolled = { _, _, _ ->
                        val firstPosition = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        val dayNumber = sessionsGroup.getDateSinceBeginOrNull(firstPosition)
                        dayNumber ?: return@addOnScrollListener
                        val dayTitle = getString(R.string.session_day_title, dayNumber)
                        binding.dayHeader.setTextIfChanged(dayTitle)
                    })
        }
    }

    private fun setDayHeaderVisibility(visibleDayHeader: Boolean) {
        val transition = TransitionInflater
                .from(context)
                .inflateTransition(R.transition.date_header_visibility)
        TransitionManager.beginDelayedTransition(binding.sessionsConstraintLayout, transition)
        val constraintSet = if (visibleDayHeader) dayVisibleConstraintSet else dayGoneConstraintSet
        constraintSet.applyTo(binding.sessionsConstraintLayout)
    }

    companion object {
        fun newInstance(): AllSessionsFragment = AllSessionsFragment()
    }
}
