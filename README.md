![Created with Android Studio](https://img.shields.io/badge/android%20studio-346ac1?style=for-the-badge&logo=android%20studio&logoColor=white)

# Race to the Magnet!
**Race to the Magnet!** is a unique Android racing application that uses a magnet as the finish line for a real-world obstacle course.

## Usage Guide

Modern smartphones contain a magnetometer, a sensor commonly used by compasses to detect magnetic fields. Race to the Magnet! uses this sensor to detect when a racer reaches a magnet.

- Create an obstacle course in real life.
- Place a magnet at the finish line, approximately at pocket level.
- Put the phone in your pocket to start the race.
  - The light sensor detects when the phone is in your pocket.
  - A notification sound alerts the racer when the timer starts.
- Run the obstacle course and reach the finish line.
  - When the phone detects the magnet, the timer stops automatically.
- Pass the phone to the next racer and repeat!

## Features
This Android application includes the following features:

- Set an image of the obstacle course you created in real life.
- **Two stopwatch modes:**
  - **Automatic:** Starts the timer when the phone detects that it is in a pocket and stops it when a magnet is detected.
  - **Manual:** Allows someone to start and stop the timer manually.
- Sensor detection to determine how accurately the magnetometer detects magnetic fields.
  - Adjust the magnetic threshold to accommodate different environmental magnetic field strengths.
- Light sensor detection to determine if the phone is in a pocket.
- Record multiple attempts from different racers.
- A results screen showing previous race history and each racer's best time.
- **Customizable settings:**
  - Configure the delay before the timer starts after pocket detection.
  - Set the default magnetic detection threshold.
  - Adjust the light sensitivity required to detect that the phone is in a pocket.
  - Choose custom notification sounds for timer alerts.

## App Screenshots
<img src="https://i.imgur.com/DvtewCD.png" alt="The main activity, where you can configure the race settings." style="width:500px">
<img src="https://i.imgur.com/2KwqHk8.png" alt="The race activity, where racers take turns to race the obstacle course." style="width:500px">
<img src="https://i.imgur.com/HwfICwu.png" alt="The history activity, where you can see result of past races." style="width:500px">
<img src="https://i.imgur.com/Z54sR7H.png" alt="The settings activity, where you can adjust default global settings." style="width:500px">
