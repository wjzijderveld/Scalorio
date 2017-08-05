# Factorio helper

I started with this tool to make a calculator for my wishes.

* Uses the Factorio data files directly, so ready for future updates
* Should have support for mods*, at least with some mods I use (Bobs, Ion Canon, possibly Angels)


_\* As long as they use the same data format as base factorio for items/recipes_

## TODO

- [x] Parse mod list
- [x] Read mod info files
- [x] Define order of loading
- [x] Appened mod paths to LUA_PATH
- [x] Parse mod data files
- [ ] Take a look at smelting/rocketbuilding
  - [ ] Possible rethink assemblers - collect items that have crafting_categories/crafting_speed
  - [ ] Add furnaces


## Roadmap

- [ ] Allow to specify location of Factorio folder (with some nice defaults)
- [ ] Allow to specify which assemblers you want to use
- [ ] Implement the above in a GUI :)
- [ ] Create a standalone JAR from it
- [ ] Create an installable from above JAR
