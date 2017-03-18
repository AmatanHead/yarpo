cmake_minimum_required(VERSION 3.5)
project(py)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fPIC")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c99 -fPIC")

set(EXECUTABLE_OUTPUT_PATH "${CMAKE_SOURCE_DIR}/build")
set(LIBRARY_OUTPUT_PATH "${CMAKE_SOURCE_DIR}/build")

add_subdirectory(pywidgets)
add_subdirectory(src)
add_subdirectory(tests)
