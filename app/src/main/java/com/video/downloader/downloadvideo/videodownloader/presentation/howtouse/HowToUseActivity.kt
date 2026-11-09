package com.video.downloader.downloadvideo.videodownloader.presentation.howtouse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseActivity
import com.video.downloader.downloadvideo.videodownloader.databinding.ActivityHowToUseBinding
import com.video.downloader.downloadvideo.videodownloader.databinding.ItemHowToUseSlideBinding
import dagger.hilt.android.AndroidEntryPoint

data class HowToUseSlide(
    val methodTitle: String? = null,
    val step1Number: Int = 1,
    val step1Title: String,
    val step1ImageRes: Int,
    val step2Number: Int? = null,
    val step2Title: String? = null,
    val step2ImageRes: Int? = null
)

@AndroidEntryPoint
class HowToUseActivity : BaseActivity<ActivityHowToUseBinding>() {

    companion object {
        const val EXTRA_PLATFORM = "extra_platform"
    }

    private var slides = listOf<HowToUseSlide>()

    override fun inflateBinding(inflater: LayoutInflater): ActivityHowToUseBinding {
        return ActivityHowToUseBinding.inflate(inflater)
    }

    override fun initViews() {
        val platform = intent.getStringExtra(EXTRA_PLATFORM)?.lowercase().orEmpty()

        binding.btnHowToUseBack.setOnClickListener { finish() }

        slides = when (platform) {
            "facebook" -> listOf(
                HowToUseSlide(
                    methodTitle = "${getString(R.string.method)} 1",
                    step1Number = 1,
                    step1Title = getString(R.string.open_facebook_and_tap_copy_link),
                    step1ImageRes = R.drawable.help_facebook1,
                    step2Number = 2,
                    step2Title = getString(R.string.open_video_downloader),
                    step2ImageRes = R.drawable.help_app_download
                ),
                HowToUseSlide(
                    methodTitle = "${getString(R.string.method)} 2",
                    step1Number = 1,
                    step1Title = getString(R.string.open_facebook_and_tap_share_to),
                    step1ImageRes = R.drawable.help_facebook2,
                    step2Number = 2,
                    step2Title = getString(R.string.open_video_downloader),
                    step2ImageRes = R.drawable.help_app_download
                )
            )
            "instagram" -> listOf(
                HowToUseSlide(
                    methodTitle = "${getString(R.string.method)} 1",
                    step1Number = 1,
                    step1Title = getString(R.string.open_instagram_and_tap_copy_link),
                    step1ImageRes = R.drawable.help_instagram1,
                    step2Number = 2,
                    step2Title = getString(R.string.open_video_downloader),
                    step2ImageRes = R.drawable.help_app_download
                ),
                HowToUseSlide(
                    methodTitle = "${getString(R.string.method)} 2",
                    step1Number = 1,
                    step1Title = getString(R.string.open_instagram_and_click_share_to),
                    step1ImageRes = R.drawable.help_instagram2,
                    step2Number = 2,
                    step2Title = getString(R.string.open_video_downloader),
                    step2ImageRes = R.drawable.help_app_download
                )
            )
            "twitter" -> listOf(
                HowToUseSlide(
                    methodTitle = "${getString(R.string.method)} 1",
                    step1Number = 1,
                    step1Title = getString(R.string.open_x_and_tap_copy_link),
                    step1ImageRes = R.drawable.help_twitter2,
                    step2Number = 2,
                    step2Title = getString(R.string.open_video_downloader),
                    step2ImageRes = R.drawable.help_app_download
                ),
                HowToUseSlide(
                    methodTitle = "${getString(R.string.method)} 2",
                    step1Number = 1,
                    step1Title = getString(R.string.open_x_and_click_share_to),
                    step1ImageRes = R.drawable.help_twitter1,
                    step2Number = 2,
                    step2Title = getString(R.string.open_video_downloader),
                    step2ImageRes = R.drawable.help_app_download
                )
            )
            "pinterest" -> listOf(
                HowToUseSlide(
                    methodTitle = "${getString(R.string.method)} 1",
                    step1Number = 1,
                    step1Title = getString(R.string.open_pinterest_and_tap_copy_link),
                    step1ImageRes = R.drawable.help_pinterest2,
                    step2Number = 2,
                    step2Title = getString(R.string.open_video_downloader),
                    step2ImageRes = R.drawable.help_app_download
                ),
                HowToUseSlide(
                    methodTitle = "${getString(R.string.method)} 2",
                    step1Number = 1,
                    step1Title = getString(R.string.open_pinterest_and_tap_share_to),
                    step1ImageRes = R.drawable.help_pinterest1,
                    step2Number = 2,
                    step2Title = getString(R.string.open_video_downloader),
                    step2ImageRes = R.drawable.help_app_download
                )
            )
            else -> listOf(
                HowToUseSlide(
                    step1Number = 1,
                    step1Title = getString(R.string.copy_the_link_from_your_browser_and_past),
                    step1ImageRes = R.drawable.help_url1
                ),
                HowToUseSlide(
                    step1Number = 2,
                    step1Title = getString(R.string.click_the_download_button_to_start_downl),
                    step1ImageRes = R.drawable.help_url2
                ),
                HowToUseSlide(
                    step1Number = 3,
                    step1Title = getString(R.string.choose_the_video_resolution_you_want_and),
                    step1ImageRes = R.drawable.help_url3
                ),
                HowToUseSlide(
                    step1Number = 4,
                    step1Title = getString(R.string.easily_play_and_manage_downloaded_videos),
                    step1ImageRes = R.drawable.help_url4
                )
            )
        }

        val adapter = HowToUseSlideAdapter(slides)
        binding.vpHowToUse.adapter = adapter

        setupDots(0)
        updateButtonText(0)

        binding.vpHowToUse.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(position)
                updateButtonText(position)
            }
        })

        binding.btnHowToUseNext.setOnClickListener {
            val current = binding.vpHowToUse.currentItem
            if (current < slides.size - 1) {
                binding.vpHowToUse.currentItem = current + 1
            } else {
                finish()
            }
        }
    }

    private fun updateButtonText(position: Int) {
        if (position == slides.size - 1) {
            binding.btnHowToUseNext.text = getString(R.string.got_it)
        } else {
            binding.btnHowToUseNext.text = getString(R.string.next)
        }
    }

    private fun setupDots(currentPosition: Int) {
        binding.layoutHowToUseDots.removeAllViews()
        val density = resources.displayMetrics.density
        for (i in slides.indices) {
            val dot = View(this).apply {
                val isSelected = (i == currentPosition)
                val width = if (isSelected) 24 else 8
                val params = LinearLayout.LayoutParams(
                    (width * density).toInt(),
                    (8 * density).toInt()
                ).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
                layoutParams = params
                setBackgroundResource(
                    if (isSelected) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                )
            }
            binding.layoutHowToUseDots.addView(dot)
        }
    }

    inner class HowToUseSlideAdapter(private val items: List<HowToUseSlide>) :
        RecyclerView.Adapter<HowToUseSlideAdapter.SlideViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
            val itemBinding = ItemHowToUseSlideBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return SlideViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class SlideViewHolder(private val itemBinding: ItemHowToUseSlideBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(slide: HowToUseSlide) {
                val density = itemView.resources.displayMetrics.density

                // Method title
                if (!slide.methodTitle.isNullOrEmpty()) {
                    itemBinding.tvMethodTitle.text = slide.methodTitle
                    itemBinding.tvMethodTitle.visibility = View.VISIBLE
                } else {
                    itemBinding.tvMethodTitle.visibility = View.GONE
                }

                // Step 1
                itemBinding.tvStep1Number.text = slide.step1Number.toString()
                itemBinding.tvStep1Title.text = slide.step1Title
                itemBinding.ivStep1Image.setImageResource(slide.step1ImageRes)

                // Step 2
                if (slide.step2Title != null && slide.step2ImageRes != null) {
                    itemBinding.cardStep2.visibility = View.VISIBLE
                    itemBinding.tvStep2Number.text = (slide.step2Number ?: 2).toString()
                    itemBinding.tvStep2Title.text = slide.step2Title
                    itemBinding.ivStep2Image.setImageResource(slide.step2ImageRes)

                    itemBinding.ivStep1Image.layoutParams = itemBinding.ivStep1Image.layoutParams.apply {
                        height = (140 * density).toInt()
                    }
                    itemBinding.ivStep2Image.layoutParams = itemBinding.ivStep2Image.layoutParams.apply {
                        height = (140 * density).toInt()
                    }
                } else {
                    itemBinding.cardStep2.visibility = View.GONE
                    // Single card slide: allow image to display larger and proportionally
                    itemBinding.ivStep1Image.layoutParams = itemBinding.ivStep1Image.layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    itemBinding.ivStep1Image.maxHeight = (280 * density).toInt()
                }
            }
        }
    }
}
