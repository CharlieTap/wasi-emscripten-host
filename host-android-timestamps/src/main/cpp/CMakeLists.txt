cmake_minimum_required(VERSION 3.22.1)
project(weh_timestamps C)

add_library(weh_timestamps SHARED timestamps.c)
target_compile_options(weh_timestamps PRIVATE -Wall -Wextra -Werror)
