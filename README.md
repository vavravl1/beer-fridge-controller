# beer-fridge-controller
A simple Kotlin based application for my own beer bridge and heating pad controller. It is based on mqtt protocol for both input and output messages

The purpose of this component is to keep my freezer at temperatures about 2-4 &#176; for beer conditioning and
to keep temperature of fermenting beer in my fridge at temperatures appropriate for the style (e.g. lagers about 10-12 &#176;C and ales about 15-20 &#176;C).

The regulation is based on a bang-bang algorithm (because of limitation of the fridges) with a simple linear prediction.
