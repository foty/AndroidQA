
###
* 内容梳理  [](../../../android/动画.md)



##### 补间动画移动后,点击事件的响应为什么还在原来的位置?
> 补间动画并没有对View的原始坐标进行修改,只是在重新绘制的时候执行Animator动画，点击事件还是之前的位置上响应。

##### 补间动画和属性动画之间的区别？
> 是否对view的原始坐标进行修改，有修改是属性动画，没有修改是补间动画。