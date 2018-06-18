#!/bin/bash

getargbool 1 rd.brltty && getargbool 1 rd.brltty.bluetooth && {
   systemctl -q is-active bluetooth || {
      hciconfig hci0 up
      systemctl --no-block start bluetooth
   }
}
