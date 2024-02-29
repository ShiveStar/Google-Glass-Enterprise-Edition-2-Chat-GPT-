This app allows you to use the Chat GPT api agent of your choice with your Google Glass EE2. To get this working do the following steps:
1. Open Key.txt and add you api key where it says "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
2. If unhappy with the current model in the file change that line as well
3. Next, use adb to push the key.txt to /sdcard. Example: adb push /path/to/key.txt /sdcard
4. Run the app! The first time it will ask for permission to access your files (to get to the key.txt) simply press allow and relaunch the app
