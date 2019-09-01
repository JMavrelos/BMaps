Project blantantly copied and improved upon https://github.com/mapbox/mapbox-navigation-android

This adds a broadcast to driving directions with action name "gr.blackswamp.bmaps.NAVIGATION" which will contain two extras 

gr.blackswamp.bmaps.NAVIGATION.DISTANCE : shows how long till next direction change
gr.blackswamp.bmaps.NAVIGATION.DIRECTIONS : shows what the next direction will be

It can also receive messages through the "gr.blackswamp.bmaps.NEW_COMMAND" action

The supported messages are:

CHANGE_ZOOM should receive a double (negative or positive) and will change the current zoom level up to the max and down to the minimum
