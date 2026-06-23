# CustomBorder

A Paper/Spigot plugin that replaces the vanilla square world border with a
custom polygon — 4 points for a rectangle/quad, or more points for any other shape.

## What it does

- You set 4 (or more) (x, z) points per world.
- Players who try to walk, fly, or teleport outside that polygon get pushed
  back, with an action-bar warning.
- Optional particle "wall" traces the polygon edges near players so the
  border is visible, like vanilla's border fog but shaped to your polygon.
- Works for concave shapes too (not just simple rectangles), since it uses
  ray-casting point-in-polygon math rather than a min/max bounding box.

## Building

Requires JDK 17+ and Maven.

```
mvn clean package
```

The built jar will be at `target/customborder.jar`. Drop it in your server's
`plugins/` folder and restart.

This targets the Paper API (works on Paper and Fork servers). If you're on
plain Spigot, it should still work — just swap the `paper-api` dependency in
`pom.xml` for `spigot-api` if you hit any issues with Adventure components.

## Setting up a rectangle (4-point) border

Stand at one corner and run, walking to each corner in turn:

```
/cborder add        (adds a point at your current location)
```

Do that 4 times, once at each corner of your rectangle (or any 4 points for
any quadrilateral shape — it doesn't have to be axis-aligned).

Then:

```
/cborder enable
```

You can also add points by exact coordinate from console or in-game:

```
/cborder add <world> <x> <z>
```

## Commands

| Command | Effect |
|---|---|
| `/cborder add [world] [x] [z]` | Add a point. No args = your current location. |
| `/cborder remove <index> [world]` | Remove point # (see `/cborder list` for numbers) |
| `/cborder list` | List points in your current world |
| `/cborder clear` | Remove all points in your current world |
| `/cborder enable` / `disable` | Turn the border on/off |
| `/cborder particles [on\|off]` | Toggle the particle wall |
| `/cborder info` | Show current settings |

Permission: `customborder.admin` (defaults to ops).

## Notes / things you may want to tweak

- **Vanilla border**: this plugin doesn't touch the vanilla world border at
  all — it runs independently. If you want to hide the vanilla border
  entirely, set it huge with `/worldborder set 60000000` so it never
  interferes visually.
- **Push-back behavior**: currently snaps players back to their last valid
  block position (simple and reliable). If you'd rather they get shoved
  toward the center of the shape instead of just stopping dead, that's a
  small change in `BorderListener.onMove`.
- **Particle type**: uses `Particle.FLAME` by default in
  `BorderParticleTask.java` — change that one line to whatever particle you
  like (e.g. `Particle.END_ROD` for a subtler look).
- **Performance**: particles only render near players (48 block radius by
  default) so it won't lag servers with large/many polygons.
- One polygon per world. If you need multiple separate zones in the same
  world, you'd want to extend `BorderManager` to key by zone name instead of
  just world name — happy to build that version if useful.
