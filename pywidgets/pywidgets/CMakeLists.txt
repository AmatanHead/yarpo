cmake_minimum_required(VERSION 3.5)
project(py)


add_custom_command(
        OUTPUT ${CMAKE_CURRENT_SOURCE_DIR}/_pywidgets.so
        COMMAND cp ${LIBRARY_OUTPUT_PATH}/_pywidgets.so ${CMAKE_CURRENT_SOURCE_DIR}/_pywidgets.so
        DEPENDS pywidgets
)

add_custom_target(pywidgets-so-link ALL DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/_pywidgets.so)
