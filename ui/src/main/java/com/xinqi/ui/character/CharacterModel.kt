package com.xinqi.ui.character

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

object CharacterModel {

    /**
     * 角色信息数据类
     */
    data class Character(
        val id: String,
        val name: String,
        val displayName: String,
        @DrawableRes val iconRes: Int,
        @DrawableRes val imageRes: Int,
        val animations: List<AnimationConfig>,
        val clickActions: List<ClickAction>,
        val bodyParts: List<BodyPart>
    )

    /**
     * 动画配置
     */
    data class AnimationConfig(
        val type: String,
        val name: String,
        @RawRes val videoRes: Int,
        val description: String
    )

    /**
     * 点击动作配置
     */
    data class ClickAction(
        val bodyPart: String,
        val position: Position,
        val clickType: ClickType,
        val response: String,
        val bluetoothCommand: String?,
        val animationTrigger: String?
    )

    /**
     * 身体部位配置
     */
    data class BodyPart(
        val id: String,
        val name: String,
        val displayName: String,
        val clickAreas: List<ClickArea>
    )

    /**
     * 点击区域配置
     */
    data class ClickArea(
        val position: Position,
        val response: String,
        val bluetoothCommand: String?,
        val animationTrigger: String?
    )

    /**
     * 位置信息
     */
    data class Position(
        val x: Float,
        val y: Float,
        val width: Float = 0.1f,
        val height: Float = 0.1f
    )

    /**
     * 点击类型枚举
     */
    enum class ClickType {
        SINGLE_CLICK,      // 单击
        LONG_PRESS,        // 长按
        RAPID_CLICK,       // 快速点击
        DOUBLE_CLICK       // 双击
    }

    /**
     * 预定义的角色配置
     */
    val CHARACTERS = listOf(
        Character(
            id = "fig1",
            name = "fig1",
            displayName = "人物1",
            iconRes = com.xinqi.ui.R.drawable.fig1_icon,
            imageRes = com.xinqi.ui.R.drawable.fig1,
            animations = listOf(
                AnimationConfig("chat", "聊天", com.xinqi.ui.R.raw.fig1_chat, "聊天动画"),
                AnimationConfig("angry", "生气", com.xinqi.ui.R.raw.fig1_angry, "生气动画"),
                AnimationConfig("shy", "害羞", com.xinqi.ui.R.raw.fig1_shy_bottom, "害羞动画")
            ),
            clickActions = listOf(
                ClickAction(
                    bodyPart = "head",
                    position = Position(0.5f, 0.2f),
                    clickType = ClickType.SINGLE_CLICK,
                    response = "别碰我头!!!",
                    bluetoothCommand = "HEAD_CENTER",
                    animationTrigger = "angry"
                ),
                ClickAction(
                    bodyPart = "body",
                    position = Position(0.5f, 0.5f),
                    clickType = ClickType.SINGLE_CLICK,
                    response = "别碰我🐻!!!",
                    bluetoothCommand = "BODY_CENTER",
                    animationTrigger = "angry"
                ),
                ClickAction(
                    bodyPart = "legs",
                    position = Position(0.5f, 0.8f),
                    clickType = ClickType.SINGLE_CLICK,
                    response = "别碰我🐔!!!",
                    bluetoothCommand = "LEGS_CENTER",
                    animationTrigger = "angry"
                ),
                ClickAction(
                    bodyPart = "legs",
                    position = Position(0.5f, 0.8f),
                    clickType = ClickType.LONG_PRESS,
                    response = "达咩",
                    bluetoothCommand = null,
                    animationTrigger = "shy"
                ),
                ClickAction(
                    bodyPart = "legs",
                    position = Position(0.5f, 0.8f),
                    clickType = ClickType.RAPID_CLICK,
                    response = "你好坏哦，我喜欢",
                    bluetoothCommand = null,
                    animationTrigger = "shy"
                )
            ),
            bodyParts = listOf(
                BodyPart(
                    id = "head",
                    name = "head",
                    displayName = "头部",
                    clickAreas = listOf(
                        ClickArea(
                            position = Position(0.0f, 0.0f, 0.3f, 0.4f),
                            response = "头",
                            bluetoothCommand = "HEAD_LEFT",
                            animationTrigger = "angry"
                        ),
                        ClickArea(
                            position = Position(0.3f, 0.0f, 0.4f, 0.4f),
                            response = "头",
                            bluetoothCommand = "HEAD_CENTER",
                            animationTrigger = "angry"
                        ),
                        ClickArea(
                            position = Position(0.7f, 0.0f, 0.3f, 0.4f),
                            response = "头",
                            bluetoothCommand = "HEAD_RIGHT",
                            animationTrigger = "angry"
                        )
                    )
                ),
                BodyPart(
                    id = "body",
                    name = "body",
                    displayName = "身体",
                    clickAreas = listOf(
                        ClickArea(
                            position = Position(0.0f, 0.4f, 0.3f, 0.2f),
                            response = "胳膊",
                            bluetoothCommand = "BODY_LEFT",
                            animationTrigger = "angry"
                        ),
                        ClickArea(
                            position = Position(0.3f, 0.4f, 0.4f, 0.2f),
                            response = "🐻",
                            bluetoothCommand = "BODY_CENTER",
                            animationTrigger = "angry"
                        ),
                        ClickArea(
                            position = Position(0.7f, 0.4f, 0.3f, 0.2f),
                            response = "胳膊",
                            bluetoothCommand = "BODY_RIGHT",
                            animationTrigger = "angry"
                        )
                    )
                ),
                BodyPart(
                    id = "legs",
                    name = "legs",
                    displayName = "腿部",
                    clickAreas = listOf(
                        ClickArea(
                            position = Position(0.0f, 0.6f, 0.3f, 0.4f),
                            response = "腿",
                            bluetoothCommand = "LEGS_LEFT",
                            animationTrigger = "angry"
                        ),
                        ClickArea(
                            position = Position(0.3f, 0.6f, 0.4f, 0.4f),
                            response = "🐔",
                            bluetoothCommand = "LEGS_CENTER",
                            animationTrigger = "angry"
                        ),
                        ClickArea(
                            position = Position(0.7f, 0.6f, 0.3f, 0.4f),
                            response = "腿",
                            bluetoothCommand = "LEGS_RIGHT",
                            animationTrigger = "angry"
                        )
                    )
                )
            )
        ),
        Character(
            id = "fig2",
            name = "fig2",
            displayName = "人物2",
            iconRes = com.xinqi.ui.R.drawable.default_icon,
            imageRes = com.xinqi.ui.R.drawable.default_icon,
            animations = listOf(
                AnimationConfig("chat", "聊天", com.xinqi.ui.R.raw.fig1_chat, "聊天动画")
            ),
            clickActions = listOf(),
            bodyParts = listOf()
        ),
        Character(
            id = "fig3",
            name = "fig3",
            displayName = "人物3",
            iconRes = com.xinqi.ui.R.drawable.default_icon,
            imageRes = com.xinqi.ui.R.drawable.default_icon,
            animations = listOf(
                AnimationConfig("chat", "聊天", com.xinqi.ui.R.raw.fig1_chat, "聊天动画")
            ),
            clickActions = listOf(),
            bodyParts = listOf()
        )
    )

    /**
     * 根据角色ID获取角色配置
     */
    fun getCharacter(id: String): Character? {
        return CHARACTERS.find { it.id == id }
    }

    /**
     * 根据角色ID获取动画配置
     */
    fun getCharacterAnimations(characterId: String): List<AnimationConfig> {
        return getCharacter(characterId)?.animations ?: emptyList()
    }

    /**
     * 根据角色ID和动画类型获取动画配置
     */
    fun getAnimationConfig(characterId: String, animationType: String): AnimationConfig? {
        return getCharacter(characterId)?.animations?.find { it.type == animationType }
    }

    /**
     * 根据角色ID和身体部位获取点击动作配置
     */
    fun getClickActions(characterId: String, bodyPart: String): List<ClickAction> {
        return getCharacter(characterId)?.clickActions?.filter { it.bodyPart == bodyPart } ?: emptyList()
    }

    /**
     * 根据角色ID获取身体部位配置
     */
    fun getBodyParts(characterId: String): List<BodyPart> {
        return getCharacter(characterId)?.bodyParts ?: emptyList()
    }

    /**
     * 检测点击的身体部位
     */
    fun detectBodyPart(characterId: String, x: Float, y: Float): BodyPart? {
        val bodyParts = getBodyParts(characterId)
        return bodyParts.find { bodyPart ->
            bodyPart.clickAreas.any { area ->
                x >= area.position.x && 
                x <= area.position.x + area.position.width &&
                y >= area.position.y && 
                y <= area.position.y + area.position.height
            }
        }
    }

    /**
     * 获取点击区域的响应信息
     */
    fun getClickAreaResponse(characterId: String, bodyPartId: String, x: Float, y: Float): ClickArea? {
        val bodyPart = getBodyParts(characterId).find { it.id == bodyPartId }
        return bodyPart?.clickAreas?.find { area ->
            x >= area.position.x && 
            x <= area.position.x + area.position.width &&
            y >= area.position.y && 
            y <= area.position.y + area.position.height
        }
    }

    /**
     * 根据点击类型和身体部位获取动画触发器
     * @param characterId 角色ID
     * @param bodyPartId 身体部位ID
     * @param clickType 点击类型
     * @return 动画触发器名称，如果没有找到则返回null
     */
    fun getAnimationTrigger(characterId: String, bodyPartId: String, clickType: ClickType): String? {
        val character = getCharacter(characterId) ?: return null
        
        // 从clickActions中查找匹配的配置
        val clickAction = character.clickActions.find { action ->
            action.bodyPart == bodyPartId && action.clickType == clickType
        }
        
        return clickAction?.animationTrigger
    }

    /**
     * 根据点击类型和身体部位获取完整的点击动作配置
     * @param characterId 角色ID
     * @param bodyPartId 身体部位ID
     * @param clickType 点击类型
     * @return 点击动作配置，如果没有找到则返回null
     */
    fun getClickAction(characterId: String, bodyPartId: String, clickType: ClickType): ClickAction? {
        val character = getCharacter(characterId) ?: return null
        
        return character.clickActions.find { action ->
            action.bodyPart == bodyPartId && action.clickType == clickType
        }
    }

    /**
     * 获取角色所有可用的点击动作
     * @param characterId 角色ID
     * @return 该角色的所有点击动作列表
     */
    fun getAllClickActions(characterId: String): List<ClickAction> {
        return getCharacter(characterId)?.clickActions ?: emptyList()
    }

    /**
     * 获取角色特定身体部位的所有点击动作
     * @param characterId 角色ID
     * @param bodyPartId 身体部位ID
     * @return 该身体部位的所有点击动作列表
     */
    fun getClickActionsByBodyPart(characterId: String, bodyPartId: String): List<ClickAction> {
        return getCharacter(characterId)?.clickActions?.filter { it.bodyPart == bodyPartId } ?: emptyList()
    }
}