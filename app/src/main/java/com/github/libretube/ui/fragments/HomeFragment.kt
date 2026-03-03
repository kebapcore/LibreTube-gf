package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.databinding.FragmentHomeBinding
import com.github.libretube.ui.adapters.VideoCardsAdapter
import com.github.libretube.ui.models.HomeViewModel
import com.github.libretube.ui.models.SubscriptionsViewModel


class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val subscriptionsViewModel: SubscriptionsViewModel by activityViewModels()

    private val homeAdapter = VideoCardsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.homeRV.adapter = homeAdapter

        with(homeViewModel) {
            trending.observe(viewLifecycleOwner, ::showTrending)
            feed.observe(viewLifecycleOwner, ::showFeed)
            isLoading.observe(viewLifecycleOwner, ::updateLoading)
        }

        binding.refresh.setOnRefreshListener {
            binding.refresh.isRefreshing = true
            fetchHomeFeed()
        }

        binding.refreshButton.setOnClickListener {
            fetchHomeFeed()
        }
    }

    override fun onResume() {
        super.onResume()

        if (homeViewModel.loadedSuccessfully.value == false) {
            fetchHomeFeed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchHomeFeed() {
        binding.nothingHere.isGone = true
        homeViewModel.loadHomeFeed(
            context = requireContext(),
            subscriptionsViewModel = subscriptionsViewModel,
            // GF Edition: Home = Türkiye trendleri
            visibleItems = setOf("trending"),
            onUnusualLoadTime = {}
        )
    }

    private fun showTrending(trends: Pair<com.github.libretube.api.TrendingCategory, com.github.libretube.ui.models.TrendsViewModel.TrendingStreams>?) {
        if (trends == null) return
        val (_, trendingStreams) = trends
        binding.homeRV.isVisible = true
        homeAdapter.submitList(trendingStreams.streams.take(30))
    }

    private fun showFeed(streamItems: List<StreamItem>?) {
        // GF Edition: subscriptions feed is not the Home source anymore
        // Keep this observer for compatibility; ignore for now.
        if (streamItems == null) return

        // no-op
    }

    private fun updateLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading()
        } else {
            hideLoading()
        }
    }

    private fun showLoading() {
        binding.progress.isVisible = !binding.refresh.isRefreshing
        binding.nothingHere.isVisible = false
        binding.homeRV.alpha = 0.3f
    }

    private fun hideLoading() {
        binding.progress.isVisible = false
        binding.refresh.isRefreshing = false

        val hasContent = homeViewModel.loadedSuccessfully.value == true
        if (hasContent) {
            showContent()
        } else {
            showNothingHere()
        }
        binding.homeRV.alpha = 1.0f
    }

    private fun showNothingHere() {
        binding.nothingHere.isVisible = true
        binding.homeRV.isVisible = false
    }

    private fun showContent() {
        binding.nothingHere.isVisible = false
        binding.homeRV.isVisible = true
    }

    private fun makeVisible(vararg views: View) {
        views.forEach { it.isVisible = true }
    }
}
