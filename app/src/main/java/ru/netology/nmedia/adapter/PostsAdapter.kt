package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.BuildConfig.BASE_URL
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAddBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.numbersToString
import ru.netology.nmedia.view.load

interface OnInteractionListener {
    fun onLikeListener(post: Post) {}
    fun onShareListener(post: Post) {}
    fun onRemoveListener(post: Post) {}
    fun onEditListener(post: Post) {}

    //  fun onPlayVideoListener(post: Post) {}
    fun onPostListner(post: Post) {}
    fun onImageListner(image: String) {}
}

class PostsAdapter(
    private val listener: OnInteractionListener
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(PostDiffCallback()) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is Ad -> R.layout.card_add
            is Post -> R.layout.card_post
            null -> error("unknow item type")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.card_post -> {
                val binding =
                    CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, listener)
            }
            R.layout.card_add -> {
                val binding =
                    CardAddBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding)
            }
            else -> error("unknow item type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is Post -> (holder as? PostViewHolder)?.bind(item)
            null -> error("unknow item type")
        }
    }
}

class AdViewHolder(
    private val bindiding: CardAddBinding,
) : RecyclerView.ViewHolder(bindiding.root) {
    fun bind(ad: Ad) {
        bindiding.image.load("${BuildConfig.BASE_URL}/media/${ad.name}")
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val listener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {

            author.text = post.author

            published.text = post.published.toString()
            content.text = post.content
            views.text = numbersToString(post.views)
            attachment.visibility = View.GONE

            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE
            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    // TODO: if we don't have other options, just remove dots
                    menu.setGroupVisible(R.id.owned, post.ownedByMe)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                listener.onRemoveListener(post)
                                true
                            }
                            R.id.edit -> {
                                listener.onEditListener(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            val url = "${BASE_URL}/avatars/${post.authorAvatar}"
            Glide.with(itemView)
                .load(url)
                .placeholder(R.drawable.ic_loading_24)
                .error(R.drawable.ic_baseline_error_outline_24)
                .timeout(10_000)
                .circleCrop()
                .into(avatar)

            val urlAttachment = "${BASE_URL}/media/${post.attachment?.url}"
            if (post.attachment != null) {
                Glide.with(attachment.context)
                    .load(urlAttachment)
                    .placeholder(R.drawable.ic_loading_24)
                    .error(R.drawable.ic_baseline_error_outline_24)
                    .timeout(10_000)
                    .into(attachment)
                attachment.isVisible = true
            } else {
                attachment.isVisible = false
            }

            like.isChecked = post.likedByMe
            like.text = numbersToString(post.likes)
            share.text = numbersToString(post.repost)

            like.setOnClickListener {
                listener.onLikeListener(post)
            }
            share.setOnClickListener {
                listener.onShareListener(post)
            }
            thisPost.setOnClickListener { listener.onPostListner(post) }

            /* if (post.video == null) {
                binding.videoGroup.visibility = View.GONE
            } else {
                binding.videoGroup.visibility = View.VISIBLE
            }
            videoBanner.setOnClickListener {
                listener.onPlayVideoListener(post)
            }
            playVideo.setOnClickListener {
                listener.onPlayVideoListener(post)
            }*/
            attachment.setOnClickListener {
                post.attachment?.let { attach ->
                    listener.onImageListner(attach.url)
                }

                menu.setOnClickListener {
                    PopupMenu(it.context, it).apply {
                        inflate(R.menu.options_post)

                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.remove -> {
                                    listener.onRemoveListener(post)
                                    true
                                }
                                R.id.edit -> {
                                    listener.onEditListener(post)
                                    true
                                }

                                else -> false
                            }
                        }
                    }.show()

                }
            }
        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        if (oldItem::class != newItem::class) {
            return false
        }
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: FeedItem, newItem: FeedItem): Any = Unit

}