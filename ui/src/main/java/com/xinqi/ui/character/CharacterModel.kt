package com.xinqi.ui.character

import androidx.annotation.DrawableRes

object CharacterModel {

    data class Character(
        val id: String,
        val name: String,
        @DrawableRes val iconRes : Int,
        val roleClickMotion: RoleClickMotion
    )

    class RoleClickMotion(
        val position: Int,
        val area: Int,
        val onClick: () -> Unit = {},
        val rapidCount: Int,
        val onRapidClick:  () -> Unit = {}
    )

}