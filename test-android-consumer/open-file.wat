;; Regenerate: wasm-tools parse test-android-consumer/open-file.wat -o test-android-consumer/src/main/assets/open-file.wasm
(module
  (import "wasi_snapshot_preview1" "path_open" (func $path_open
    (param i32 i32 i32 i32 i32 i64 i64 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_close" (func $fd_close (param i32) (result i32)))
  (memory (export "memory") 1)
  (data (i32.const 16) "probe.txt")
  (func (export "run")
    ;; Create a file in the first preopened directory (fd 3), with FD_WRITE rights.
    (call $path_open
      (i32.const 3) (i32.const 0) (i32.const 16) (i32.const 9)
      (i32.const 1) (i64.const 64) (i64.const 0) (i32.const 0) (i32.const 0))
    if unreachable end
    (call $fd_close (i32.load (i32.const 0)))
    if unreachable end))
