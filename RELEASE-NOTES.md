# Datavyu 1.5.2 Release Notes
## 03/15/2021
### Notable Changes
* Changed Mac OS Hide/Show track to * numpad key
* Changed Mac OS Point Cell to / numpad key

# Datavyu 1.5.2-RC1 Development Release Notes
## 02/02/2021
### Notable Changes
* Default to Native OSX plugin on Mac OS
* Removed Datavyu Video Converter

### Bug Fixes
* Fixed Shortcuts errors in the Shortcuts list window
* Fixed seeking to end of video when dragging the track
* Fixed crashes on loading Datavyu plugins

# Datavyu 1.5.1-RC3 Development Release Notes
## 11/20/2019
### Notable Changes
* (Playback) Replaced steps per second by a new Shortcut to jump forward by time ( SHIFT - )
* (Playback) Replaced "Steps per Second" by "Frame Rate".
* (Playback) Disabled "Frame Rate" field and Added FPS label for each track.
* (Playback) Faster FFmpeg player frames rendering.
* (Playback) Improved FFmpeg player seeks accuracy.
* (Ruby API) Improved ability to add and remove videos.

### Bug Fixes
* (Playback) Fixed resizing video via the controller.
* (Playback) Fixed Native OSX player speed transitions.
* (Playback) Fixed Native OSX jogging when track offset is positive.
* (Playback) Fixed crashes when destroying FFmpeg player.
* (Playback) Fixed FFmpeg player seeks to end of the video.
* (General) Fixed Datavyu crashes when no Internet connection.
* (General) Fixed Datavyu Mac menubar highlight on mouse hover.
* (General) Fixed OPFs backward compatibility with previous Native OSX plugins.
* (General) Removed ghost window opening up with mac FFmpeg player.
* (General) Show mouse cursor on FFmpeg plugin video window.
* (General) Select correct cell's code when highlight and focus is enabled.

### Known Issues
* Shortcuts not working on Mac FFMPEG Window.
---

# Datavyu 1.5.0 Release Notes
## 07/26/2019
### Notable Changes
* Removed JavaFX plugin.
* Removed MPV plugin.
* Major improvements to FFMPEG plugin.
* Add FFMPEG as default plugin for Mac and Windows
  * Users will receive a deprecation notice when opening Datavyu files which
  were using removed plugins. Videos will automatically re-open using FFMPEG.
* Re-work highlight and highlight and focus for more intuitive use.
* New installations will prompt users to share usage data.
* (ruby api) add function to save and load track information to/from a spreadsheet column.


### Bug Fixes
* (general) fix crashing when VLC is installed.
* (general) fix memory leaks when removing videos.
* (general) fix startup crash when no network connection is available.
* (playback) fix jogging outside of locked region.
* (ruby api) compute_kappa won't crash on edge case.
* (ruby api) fix sanitization of code names when they start with a number.

### Known Issues
* Controller track video resize buttons do not work.
* Mouse disappears in video window.
* Ghost window opens up on Mac FFMPEG.
* Shortcuts not working on Mac FFMPEG Window.
---

# Datavyu 1.4.2 Release Notes
## 11/19/2018

### New Features
* Introduced MPV plugin for Windows:
    * Added support for 1080p Resolution.
    * Playback speed from -32x to +32x for Images and Audio.
    * Accurate and consistent seeks.
    * Stepping frame by frame forward and backward.
* FFmpeg plugin new features:
    * Added support for 1080p Resolution.
    * Added support for playback speeds from 0x to 32x for Images and Audio.
    * Improved the seek accuracy.
    * Stepping frame by frame forward and backward.
    * Keep aspect ratio when resizing FFmpeg plugin window.
* Added new Ruby API function to load a video from a script.
* Re-arrange column from the Code Editor dialog.

### Bug Fixes
* Fixed installation issues on older Mac OS versions.
* Fixed unselected spreadsheet cells when clicking on the Video Controller window.
* Load the correct plugin from the OPF file.
* Fixed incorrect time when inserting cells on subsequently locked regions.
* Seek to cells' onset when snapping the cell.
* Fixed delete column from the menu bar.
* FFmpeg plugin for Windows
    * Fixed FFmpeg plugin crashes when stressed.
    * Display the first image when the video is loaded.
    * Update images when seeking and the stream is stopped.
    * Enable seeks before the first play.
    * Fixed Synchronization problems between audio and images.

### Known Issues
* FFmpeg plugin for Windows
    * Backward playback is not performing as expected.
    * The Plugin will not update the image when seeking to the end of the track.
    * Seeking (either through Find or synchronization when stopping) does not always produce same frame. May seek to adjacent frame.
    * Inconsistent frame reporting between plugins. We suggest using the same plugin when coding to maintain consistency.

---

# Datavyu 1.4.1 Release Notes
## 06/28/2018

### New Features
* FFMPEG plugin for Windows.
    * Upgraded to FFmpeg 4.0 version.
    * Added Support for 720p Resolution.
    * Plugin still under development, expect performance issues with high-resolution videos.
* Enabled cells editing when highlight and focus is enabled and video is stopped.
* Changed JavaFX plugin icon.
* Improved Cell highlighting while seeking.
* Added Undo feature for cells created using Quick Key mode.

### Bug Fixes
* Fixed flickering issues when clicking on Hidden Columns.
* Fixed `CMD+L` and `CMD+R` bug for corner cases.
* Fixed add cell to left/right columns not working.
* Fixed Video Controller back button not working for corner cases.
* Fixed unresponsive Spreadsheet when using the Menu on Windows.
* Keep track of code position when moving through cells within a column.
* Fixed choppy backward playback with Native OSX Plugin.
* Fixed cycling through codes in a cell bug.
* Fixed FFmpeg plugin Windows not resizing on Windows.
* Fixed FFmpeg seek issue.
* Display the correct Frame Rate per Second (FPS) for the javaFX plugin.
* Fixed Dialog windows appearing behind the video.

### Known Issues
* #### When using the ffmpeg plugin:
    * sound only plays back when playing at 1x.
    * jumping to random locations within the stream may take 1-2sec
    * the sound after a stop/jump and playback may be a bit distorted when opening multiple streams and the ffmpeg as 2nd, 3rd it might get stuck after playing for some time
    * Inconsistent frame reporting between plugins. We suggest using the same plugin when coding to maintain consistency.

*  #### Delete column menubar function does not work. Use the Code Editor to delete columns.

---

# Datavyu 1.4.0 Release Notes
## 04/26/2018

### New Features
* FFMPEG plugin for Windows.
  * Still under development. Currently only supports 480p video. Expect performance issues with higher quality files.
* Removed plugins not being maintained (e.g., VLC, MPlayer).
* Move left and right across columns using `CMD + Left Arrow` and `CMD + Right Arrow` on Mac and `CTRL + Left Arrow` and `CTRL + Right Arrow` on Windows.
* Import/Export coding passes from/to JSON format.
* Display foreign language characters (can not input through keystrokes yet).
* `CMD + W`, `CTRL + W` closes currently open spreadsheet on Mac and Windows, respectively.
* Updated dependencies for R and Ruby script plugins

### Bug Fixes
* Allows to save filenames with non-ascii characters, e.g. Umlaut in German
* Fixed plugin loading error for NativeOSX plugin.
* OSX plugin playback more responsive to jogs, cell locking, and seeks.
* Fixed flickering during Cell Highlighting.
* Fixed delete cell shortcut not working on Windows.
* Fixed Code Editor not properly editing code/column names.
* Recognize playback keys even when Spreadsheet window is not in focus.
* ##### Ruby API
  * Fixed `show_columns` crash.
  * Fixed `save_db` crash.

### Known Issues
* #### When using the ffmpeg plugin:
  * the window can't be resized
  * sound only plays back when playing at 1x
  * jumping to random locations within the stream may take 1-2sec
  * the sound after a stop/jump and playback may be a bit distorted
  * when opening multiple streams and the ffmpeg as 2nd, 3rd it might
    get stuck after playing for some time
  * Inconsistent frame reporting between plugins. We suggest using the same plugin when coding to maintain consistency.
* #### Ruby API
  * Scripts may crash unexpectedly with error reported in `getAllVariables` functions. We suggest re-running the script and if the problem persists, re-opening Datavyu.

---

# Datavyu 1.3.7 Release Notes
## 07/18/2017

### New Features
* Larger column header and drag-and-drop boundary.

### Bug Fixes
* Fix code editor issues with editing and deleting codes.
* Fix data playback outside of locked region.
* UI updates properly after saving.

### Known Issues
* There is still an issue with saving, but it should be very rare.
  As a workaround, use the sample script [SaveFile](https://github.com/databrary/Datavyu-Example-Scripts/blob/master/Misc/SaveFile.rb)
  to save files when encountering this issue.

---

# Datavyu 1.3.6 Release Notes
## 4/3/2017

### New Features
* Highlight and focus mode
    - Cells with highlight green when the playback timer is within a cell,
and red when the playback timer is past a cell.
    - Enables easy visualization of which cells are active and which are
not at the current playback time.
* Quick key mode
    - Select a column and enable quick key mode. Now, whenever you press an
alphanumeric key, a cell will be added to that column at the current time
with the first code filled in with the key that you pressed.
    - Makes doing quick passes over a column easier than ever!
* Default OSX media player is now based on AVFoundation instead of QTKit,
improves performance and allows GPU acceleration

### Bug Fixes
* Fixed some issues with playback speed on OSX when backwards shuttling
* On old versions of OSX, the QTKit player will be loaded instead of the
AVFoundation player to ensure that playback still works (pre-10.6).
* Other small tweaks.

### Ruby API 1.3.6 release notes

* Added standalone functions:
    - check_valid_codes2
    - check_valid_codes3
    - combine_columns
    - compute_kappa
    - get_datavyu_files_from
    - get_datavyu_version
    - hide_columns
    - merge_columns
    - show_columns
* Added functions to RCell:
    - get_codes
    - overlapping_region
    - overlaps
    - overlaps_range
    - spans
* Added functions to RColumn:
    - resample
* Renamed RVariable to RColumn
* Renamed 'arg' to 'code'
* Renamed 'variable' to 'column'
* Added CTable class (used for computing kappa score).
* Renamed functions for internal consistency and to follow Ruby Style Guide.
    * snake-case; e.g. new_column() instead of newColumn()
    * Previous names should work as well. If they do not please report them
on the [support forum](http://datavyu.org/support) or e-mail info@datavyu.org

See the [reference page for more information](http://datavyu.com/user-guide/api/reference.html)

### Bug Fixes
  * create_mutually_exclusive prints a single line of output to scripting
console instead of three lines
  * load_macshapa_db sanitizes code names and substitutes '#' with 'number'
and '&' with 'and'

---

# Datavyu 1.3.3 Pre-Release Notes
## 07/14/2015

### Additions
* Datavyu should start up faster

### Bug Fixes
* Datavyu's Export File by Frame script now works on files with empty columns
* Drag and drop onto the application is fixed since 1.3.1
* New improvements to the undo feature
* Locking into a cell now stops the video at the offset

### Known Issues
* Dragging and dropping a file onto the application on PC's creates a new Datavyu window

---

# Datavyu 1.3 Pre-Release Notes
## 3/16/2015

### Additions
* Datavyu's packaging now includes Java so users no longer have to download it separately
* Added a progress bar to the video conversion tool
* Highlighting can now show you where your video is playing across the spreadsheet. Enable highlighting on the Controller.
* Check out Karen Adolph's [Best Practices for Coding Behavioral Data](http://datavyu.org/user-guide/best-practices.html) on our User Guide

### Bug Fixes
* Datavyu now works on OS X 10.10 Yosemite!
* Adding a coded spreadsheet to Datavyu will replace the Untitled Blank document to eliminate potential saving issues
* Closing tabs no longer requires additional saving
* Adding cells to the right or left keeps the spreadsheet focus
* Windows: initial video frame should be seen on the video

### Known issues
* Hitting undo after running a script causes the spreadsheet to delete data
* Video lag might occur with high definition videos. Please try out the different video plugins to see which works best for your videos.

# Announcements
## 1/20/2015

* Yosemite no longer supports Java QuickTime and so Datavyu is not currently compatible. We apologize for the inconvenience     and advise our users to be aware of the situation before deciding to upgrade to Yosemite.

* If you try to download Datavyu and receive a pop up saying: "Datavyu is damaged and should be moved to the trash," please    check your security settings to allow the download of the software. Please go to System Preferences/Security/General tab     and then check "Allow apps to be downloaded from: anywhere."

* Can't seem to hold down jog while navigating your video? All you have to do is go to your Terminal (found in your Hard       Drive/Applications/Utilities folder) and enter the following: defaults write -g ApplePressAndHoldEnabled -bool false

---

# Datavyu 1.2.2 Pre-release Notes
## 9/25/14

### Additions
* We added a sample video and spreadsheet to our download page so users can practice coding
* The user guide is now in PDF form
* Opening a spreadsheet replaces the original blank spreadsheet on windows and soon on mac
* Keyboard shortcuts list is available in the help drop down menu of datavyu

### Bug Fixes
* You can have a file, save it as a different name and open both files in datavyu
* Point cell no longer changes the previous cell’s offset
* Undo history works when adding a cell
* You can no longer open two of the same spreadsheets at once

### Known Issues
* Locking into a cell sometime doesn’t work
* Adding and closing multiple spreadsheets in datavyu sometimes makes you save an extra time
* Temporal alignment sometimes aligns cells that are a few milliseconds off

---

# Datavyu 1.2 Release Notes
## 06/30/2014

### Additions
* Choose your own favourites folder: will determine contents of bottom-left file panel, script dropdown menu
* Frame-by-frame export is now available in the drop down menu
* You can now open multiple spreadsheets in datavyu! Each spreadsheet has its own corresponding controller. Tabs at the top of the spreadsheet are used to navigate between open spreadsheets.
* We have added our user guide, release notes, and developer notes to the application folder.
* You can manually set the jog rate of a video on the Controller. Please double click on “Steps per second”, type in your new frame rate and then click “Enter”.
* When you “Add Data” with the controller, it defaults to finding “Video Files”. If you cannot find your desired file, you can simply change the format to “All Files”.
* New hidden column count on the spreadsheet
* Up and down arrow keys allow you to move up and down cells
* Stable name restrictions in code editor and spreadsheet


### Bug Fixes
* Undo works correctly for column deletions
* Application title bar now always reflects the name of the current project
* Fixes to scripting API: mutex, transfer columns
* Combine columns script function works

### Known Issues
* When renaming a file with "Save as" and trying to open the original file, loading sometimes fails with a wrongful "This file is already open" error.
* VLC is not fully implemented. Please try playing your videos in QuickTime.
* Undo for cell creation is not reliable
* PC's with CPU's of less than 2.67 GHz have choppy video playback when playing at full speed
* You can drag a code name within a cell but it does not influence the code itself and it does not save. Please save your file and reopen it.
* Scripting output sometimes does not give you the proper output. You can cross check your scripts in OpenSHAPA.

---

# Datavyu 1.1 Release Notes
## 03/10/2014

### Additions
* Created more prompts to save to prevent accidental data loss.
* Minimizing the spreadsheet window will minimize all Datavyu windows.

### Changes
* The names of some software features were changed to adhere to a more intuitive naming scheme:
    * "Vocab Editor" --> "Code Editor"
    * "Observation" --> "Cell"
    * "Argument" --> "Code"
    * "Temporal Ordering" --> "Temporal Alignment"
    * "Variable" --> "Column"
    * "Data Viewer" --> "Media Player"
    * "Data Viewer Controller" --> "Controller"
* Quicktime 7.7.5 no longer includes java libraries, but you can easily include them in your download. For new installs and for Quicktime updates, please follow the steps provided on the [installation page](http://www.datavyu.org/user-guide/_images/install-quicktime-special.png).
* The [Controller](http://www.datavyu.org/user-guide/_images/keypad-add-data-button.png) has a new format that is no standard between PC and Mac keyboards.
* Users are now restricted to Matrix-style columns. Text and nominal columns can no longer be created but pre-existing instances from saved files are still supported. Since it is now the only type of column, matrices are now referred to simply as columns.
* Restrictions on column names have been enforced to prevent problems in the scripting interface.
    * Column names must begin with a letter.
    * Only letters, numbers and underscores are allowed in column names.
    * Previously created columns that do not adhere to these restrictions will continue to work, but new column names (or name changes) will be restricted.
    * We have provided a script in favorites folder that will turn periods into underscores in existing columns to aid the transition.
* Datavyu is no longer uses Mongo DB. Eliminating this outside dependency means:
    * Datavyu is less memory intensive.
    * The Datavyu application is smaller.
    * Startup and file loading are faster.
    * Errors when closing Datavyu on Windows machines have been fixed.

### Bug Fixes
* Fixed issues causing cells to overlap when temporal alignment is turned on.
* When running scripts, more informative status messages are printed to the console window.
* You no longer have to scroll when you code a long spreadsheet.
* Column location in the spreadsheet is saved.
* Hide/Show columns list is saved.

### Known Issues
* The arrow keys do not always allow you to move from code to code in the code editor and spreadsheet but the tab and shift tab keys have the same function.
* Undo history is not always accurate.
* You can click, hold down, and drag a code name within a cell but it does not influence the code itself and it does not save. Please save your file and reopen it.
* Occasionally, running a script will produce no output when in fact, the script did work.
* The changed file marker (*) doesn't always disappear on save.
* VLC cannot accurately determine frame rates of our videos and thus is unsupported in Datavyu.




# Datavyu 1.04rev3 Release Notes
## 12/11/2013

### Additions
* Added file association support in Mac and Windows. Double-clicking on a .opf file will now open the file in Datavyu.

### Changes
* "Variables" have been renamed "columns" and "matrix arguments" have been renamed "codes" throughout the user interface.
* All API functions have been renamed to use the words "column" and "code", however, API functions using the old naming scheme will continue to work so that scripts do not need to be revised.
* Scripts now allow the user to overwrite an existing column. Previously, trying to set a column back to the spreadsheet caused an error if the columns shared the same name.

### Bug Fixes
* Fixed an issue with Datavyu failing to close completely (preventing the user from being able to reopen the program).
* Corrected a case where setting the offset of a cell changed the offset of the previous cell in a column
* Fixed the create\_mutually\_exclusive script to correct an error where the final cell was not written to the spreadsheet.
* Fixed a bug that was preventing output from being written to the scripting console.
* Improved video playback performance issues on Windows and Mac.
* Fixed some cases where cells failed to display correctly when temporal alignment is active.

### Known Issues
  * In some cases, the asterisk in the title bar indicating that changes have been made to the file may not appear/disappear correctly
  * Hidden variables may be listed as shown in the variable list
  * Temporal alignment displays cells incorrectly in rare cases
  * Windows will not correctly handle opening a file by double clicking it once Datavyu is already open.

# Datavyu 1.0.3 Release Notes
## 9/19/2013

### Additions
  * Datavyu automatically checks for updates when opened
  * On file open, a window pops up to display file open progress
  * Ruby scripting API is documented on the support wiki
  * Errors print to the scripting console window
  * A message prints to scripting console window to indicate that a script is running

### Changes
 * Scripts must not include the entire API at the top of the file. Instead, add the line _require ‘Datavyu\_API.rb'_ to the top of the file before the _begin_ command. Please see the support wiki for details.

### Bug Fixes
  * Fixed change_arg and make_rel functions in the scripting API
  * Prevented Datavyu from writing special characters that made files unreadable in OpenSHAPA
  * Fixed a bug where some offsets would be set to 00:00:000 when the file was saved
  * Fixed the delete cell command
  * Performance improvements when opening files
  * Fixed memory issues that caused a drop in video playback performance

### Known Issues
  * Video playback on Windows may be laggy
  * In some cases, the asterisk in the title bar indicating that changes have been made to the file may not appear/disappear correctly
  * Hidden variables may be listed as shown in the variable list
  * Temporal alignment displays cells incorrectly in rare cases
