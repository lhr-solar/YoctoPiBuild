# YoctoPiBuild

## Dependencies
  Download Ubuntu with WSL and clone the repo.\
  Download [RPIBoot](https://www.raspberrypi.com/software/)

## Build
  Clone the repo cd into it 
  ```
   git clone https://github.com/lhr-solar/YoctoPiBuild
   cd YoctoPiBuild/
  ```
  Build command
  >This will take a while at first to download the dependencies and then compile them but only on inital build
  ```
   ./kas-container build kas-cm5.yml
  ```
  After the build we want to move the zip build into the desktop so we can flash 
  >you can also mount the PI via usbipd-win and then flash w/bmap-tools but routing the usb every flash is a hassle
  ```
  // make sure to change <Insert Windows User Name> with the correct user path
  cp build/tmp/deploy/images/raspberrypi-cm5-io-board/photon-image-raspberrypi-cm5-io-board.rootfs.wic.bz2 /mnt/c/Users/<Insert Windows User Name>/Desktop
  ```
  Extract it from your desktop (its just a zip file) \
  boom u have the image

## Flash 
  Launch the rpiboot-CM4-CM5 - Mass Storage Gadget 
  Wait for it to connect to the eemrc
  Launch Raspberry Pi Imager
  Select cm5 and then the image we have from our desktop
  Just flash then.
  
