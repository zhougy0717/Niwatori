# Niwatori

Thanks to [tkgktyk](https://forum.xda-developers.com/member.php?u=5692104) for creating Niwatori module. This is the best one handed app/module I ever used.

Thanks to [rovo89](https://forum.xda-developers.com/member.php?u=4419114) for bringing Xposed Framework to us and a lot of fantastic modules.

# Introduction

Niwatori was a module that created by [tkgktyk](https://forum.xda-developers.com/member.php?u=5692104). The original XDA discussion thread is [here](https://forum.xda-developers.com/xposed/modules/mod-niwatori-fly-apps-window-t3031680). The original module supports up to Android 6.0 (mashmallow). Due to some reason, he decided to leave Xposed Framework. So there won't be refreshment on the original module. 

I forked the [tkgtkyk's](https://github.com/tkgktyk/Niwatori) code on GitHub and start maintaining it from Nougat. Here are my links:

- [GitHub repo](https://github.com/zhougy0717/Niwatori)
- [XDA thread](https://forum.xda-developers.com/xposed/modules/mod-niwatori-one-handed-mode-t3730963)
- [Xposed Repo](http://repo.xposed.info/module/cn.zhougy0717.xposed.niwatori)

The current status is it can support Nougat and I also bring some improvement based on my daily experience.

## How to use it?

There is a cool video in the [original Niwatori XDA thread](https://forum.xda-developers.com/xposed/modules/mod-niwatori-fly-apps-window-t3031680) for how to use it. There are 3 handy modes for you to operate on your phone with one hand. They are:

- Small screen - Which will bring the whole screen into 1/4 window at left/right bottom.
- Movable screen - Which will enable you to drag the whole screen everywhere with original layout.
- Slide down - Which pull the upper half screen downside into bottom half.

And it also support using these modes with status bar.

To enable each mode, you have several choices:

- Create a shortcut on your home screen, and then open it. This is not that useful. 
- Bind the shortcut with other tools:
  - Most ROM's provide binding shortcut onto hardware/virtual buttons.
  - You can bind it with PIE control in Gravity Box
  - You can bind it with fooview.
  - Any other tools that can invoke shortcut functionality.

# Release Note

## v0.5.2

1. Improve the compatibility.
2. Merge the shared preference solutions in Slim7 (AOSP based) and Oxygen OS.

## v0.5.1

1. Fix the white background in Nova launcher.
2. Fix shared preference issue on Oxygen OS.

## v0.5.0

1. Fix the background graphical issue.
2. Enhance animation speed.
3. Bring the flying features to big popup window.
4. Fix issues in some apps.
5. Add 3 shortcuts of major functionalities to work with fooview.

## v0.4

Original development by tkgtkyk.



