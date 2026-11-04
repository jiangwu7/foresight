# Regenerate STLs and preview renders from adapter.scad.   make            (needs openscad on PATH)
OPENSCAD ?= openscad
PARTS = clamp illumination rail carriage eyecup assembly

all: $(addprefix stl/,$(addsuffix .stl,$(PARTS))) renders/assembly.png renders/exploded.png renders/illumination.png

stl/%.stl: adapter.scad
	@mkdir -p stl
	$(OPENSCAD) -o $@ -D 'part="$*"' $<

renders/assembly.png: adapter.scad
	@mkdir -p renders
	$(OPENSCAD) -o $@ --imgsize=1200,1400 --camera=220,-260,120,0,0,70 --projection=p --colorscheme='Tomorrow Night' -D 'part="assembly"' $<

renders/exploded.png: adapter.scad
	@mkdir -p renders
	$(OPENSCAD) -o $@ --imgsize=1200,1600 --camera=260,-300,160,0,0,110 --projection=p --colorscheme='Tomorrow Night' -D 'part="exploded"' $<

renders/illumination.png: adapter.scad
	@mkdir -p renders
	$(OPENSCAD) -o $@ --imgsize=900,900 --camera=90,-110,70,0,0,20 --projection=p --colorscheme='Tomorrow Night' -D 'part="illumination"' $<

safety:
	python safety_calc.py

.PHONY: all safety
