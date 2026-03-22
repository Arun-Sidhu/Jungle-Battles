# This Makefile keeps the usual compile and run steps in one place so you can
# build the project without retyping long commands every time.

MAIN_CLASS := junglebattles.ui.JungleBattlesGame
SRC_DIR := src
OUT_DIR := out
RES_DIR := resources

JAVA := java
JAVAC := javac
FIND := find

# This gathers every Java source file under src so the compile step stays up to
# date as the project grows.
SOURCES := $(shell $(FIND) $(SRC_DIR) -name '*.java')

.PHONY: all compile run clean rebuild images help

# This makes plain make behave like make compile.
all: compile

# This target compiles every source file into the out folder.
compile: $(OUT_DIR)/.compiled

# This stamp file gives make something concrete to track after a successful
# build. The out folder is created if it does not already exist.
$(OUT_DIR)/.compiled: $(SOURCES)
	@mkdir -p $(OUT_DIR)
	$(JAVAC) -d $(OUT_DIR) $(SOURCES)
	@touch $(OUT_DIR)/.compiled

# This runs the game with the compiled classes and resources folder on the
# classpath so the images can be loaded at runtime.
run: compile
	$(JAVA) -cp $(OUT_DIR):$(RES_DIR) $(MAIN_CLASS)

# This removes compiled output so you can force a fresh build.
clean:
	rm -rf $(OUT_DIR)

# This is a convenience target for a full clean rebuild.
rebuild: clean compile

# This prints the image names the program expects in resources/images.
images:
	@echo "Expected image files in $(RES_DIR)/images"
	@echo "  Antelope.png"
	@echo "  Elephant.png"
	@echo "  Hyena.png"
	@echo "  Lion.png"
	@echo "  Rhino.png"
	@echo "  Zebra.png"

# This gives a quick reminder of the available targets.
help:
	@echo "Available targets"
	@echo "  make or make compile   Compile the project"
	@echo "  make run               Compile if needed and launch the game"
	@echo "  make clean             Remove compiled output"
	@echo "  make rebuild           Clean and compile again"
	@echo "  make images            Show the expected image filenames"
