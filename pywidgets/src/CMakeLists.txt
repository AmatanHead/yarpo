cmake_minimum_required(VERSION 3.5)
project(py)


include_directories(../include)


find_package(PythonLibs REQUIRED)

include_directories(${PYTHON_INCLUDE_DIRS})


find_package(Qt5Core REQUIRED)
find_package(Qt5Widgets REQUIRED)

include_directories(${Qt5Core_INCLUDE_DIRS} ${Qt5Widgets_INCLUDE_DIRS})


add_library(widgets widgets.cpp widgets_internal.h ../include/widgets.h)

set_target_properties(widgets PROPERTIES OUTPUT_NAME widgets)

target_link_libraries(widgets ${PYTHON_LIBRARIES})
target_link_libraries(widgets ${Qt5Core_LIBRARIES} ${Qt5Widgets_LIBRARIES})


add_library(pywidgets SHARED pywidgets.cpp widgets_internal.h ../include/widgets.h)

set_target_properties(pywidgets PROPERTIES OUTPUT_PATH build)
set_target_properties(pywidgets PROPERTIES OUTPUT_NAME _pywidgets)
set_target_properties(pywidgets PROPERTIES PREFIX "")
set_target_properties(pywidgets PROPERTIES SUFFIX ".so")

target_link_libraries(pywidgets ${PYTHON_LIBRARIES})
target_link_libraries(pywidgets ${Qt5Core_LIBRARIES} ${Qt5Widgets_LIBRARIES})

target_link_libraries(pywidgets widgets)
