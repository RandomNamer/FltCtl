# FltCtl

Got its job done, now a full-blown test project. 

It's somewhat ugly therefore:

- Single module: in future I might need to split them, but not today. 
- Ton of permissions: Don't bother to write any runtime permission code.
- All the dependencies you dont need: for various tests and scripting. 
- Logs a lot: logger backed by a mmap file, easily spit 1k lines of log to your disk.
- UI from a Material3 and Jetpack Compose learner in 2021. 

## TODO

- Use secure setting to modify daltonizer to -1 to achieve grayscale
- Revamp app list config and activity-based config.
- Manual daltonizer control
- Split home page states to display user switch and granted permissions.
