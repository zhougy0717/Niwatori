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

## Known Issues

1. Because some ROM's, for example Oxygen OS, have their own auto boot control. Niwatori need to be in the white list to provide swap left/right function with auto boot permission. 
2. Shared preference .xml is not created at the very first time after installation. You need to open Niwatori to have it create it.
3. In Oneplus Oxygen 5 (Oreo), you need to put Niwatori in Battery optimization "Not optimized" list. Otherwise, the persistent small screen mode won't work. Other functionalities work well. I don't see this happen on Pixel stock image.

# Release Note

## v0.6.2

1. Re-arch all one-hand-mode handlers. Now they inherited from one base class.
2. Remove support for extra actions. This once was designed for manipulating notification panels. Now you can use notification panel gesture to do that.
3. Add switch for enabling/disabling triggering gesture.
4. Bug fix.

## v0.6.1

1. Add global triggering gesture for small screen mode. Now you can swipe on left or right edge to trigger small mode in any Activity window.
2. Fix the background graphical issue for notification panel.
3. Other minor bug fixes.

## v0.6.0

There are bunch of new features in this version. So I bump it up to a big version change.

1. The content view in small screen mode is moved away from the edge with margins in both X and Y axis.
2. You can use gestures in those margins. Be noted that this gesture still doesn't work with Notification panel. I will work on this soon. The gestures include:
   - slide down to slide the upper half of the shrunk content down to bottom half.
   - slide up to reset to original shrunk screen position
   - scroll left to zoom out
   - scroll right to zoom in
3. Add a tutorial guide in the first app launch. You can also open it up with a menu item.
4. Translate into Chinese simplified.
5. Remove all the donation limitation. You can use every premium feature free.

## v0.5.4

1. Add double tap gesture on notification panel. You can play with Notification panel much easier with Niwatori.
2. Fix background issue in Discord and Taobao.

## v0.5.3

1. Compile with Oreo (API 26).
2. Swap directly instead of broadcasting intent to work better in Oreo.
3. Enhance compatibility.

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



