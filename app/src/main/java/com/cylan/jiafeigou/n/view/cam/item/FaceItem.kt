package com.cylan.jiafeigou.n.view.cam.item

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import com.cylan.jiafeigou.R
import com.cylan.jiafeigou.support.photoselect.CircleImageView
import com.cylan.jiafeigou.utils.AvatarRequest
import com.mikepenz.fastadapter.items.AbstractItem

/**
 * Created by yanzhendong on 2017/10/9.
 */
class FaceItem : AbstractItem<FaceItem, FaceItem.FaceItemViewHolder>() {

    companion object {
        const val FACE_TYPE_ALL = -1
        const val FACE_TYPE_STRANGER = 0
        const val FACE_TYPE_ACQUAINTANCE = 1
    }

    override fun getViewHolder(v: View): FaceItemViewHolder {
        return FaceItemViewHolder(v)
    }

    var faceText: String? = null
    var faceId: Int = 0
    var faceType: Int = 0 //熟人或者陌生人

    var faceIcon: String? = null //图片地址

    @SuppressLint("ResourceType")
    override fun getType(): Int {
        return R.layout.item_face_selection
    }


    override fun getLayoutRes(): Int {
        return R.layout.item_face_selection
    }

    override fun bindView(holder: FaceItemViewHolder, payloads: MutableList<Any>?) {
        super.bindView(holder, payloads)
        holder.text.text = faceText ?: "小明"
        //todo 全部是默认图,陌生人是组合图片,需要特殊处理
        when (faceType) {
            FACE_TYPE_ALL -> {
                //todo UI图导入
                holder.icon.setImageResource(R.drawable.news_icon_all_selector)
            }
            FACE_TYPE_STRANGER -> {
                //todo 多图片合成
                //http://img.taopic.com/uploads/allimg/120727/201995-120HG1030762.jpg
                val avatarRequest = AvatarRequest.Builder()
                        .addAvatar("http://img.taopic.com/uploads/allimg/120727/201995-120HG1030762.jpg")
                        .addAvatar("http://img.taopic.com/uploads/allimg/120727/201995-120HG1030762.jpg")
                        .addAvatar("http://img.taopic.com/uploads/allimg/120727/201995-120HG1030762.jpg")
                        .build()
                Glide.with(holder.itemView.context)
                        .load(avatarRequest)
                        .placeholder(R.drawable.news_icon_stranger)
                        .error(R.drawable.news_icon_stranger)
                        .into(holder.icon)
                holder.icon.isDisableCircularTransformation = true
                holder.icon.showHint(true)
            }
        //todo 可能会有猫狗车辆行人,这些都是预制的图片,需要判断
            else -> {
                Glide.with(holder.itemView.context)
                        .load(faceIcon)
                        .into(holder.icon)
            }
        }

    }

    class FaceItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: CircleImageView = view.findViewById(R.id.img_item_face_selection) as CircleImageView
        val text: TextView = view.findViewById(R.id.text_item_face_selection) as TextView

    }
}