'use strict';

var application = {};

application.gameView = function (svg, color, options) {
    var self = this;

    options = options || {};

    var cellWidth = options.hasOwnProperty('cellWidth') ? options.cellWidth : 21;
    var cellHeight = options.hasOwnProperty('cellHeight') ? options.cellHeight : 21;
    var figureSize = options.hasOwnProperty('figureSize') ? options.figureSize : 10;
    var pointerSize = options.hasOwnProperty('pointerSize') ? options.pointerSize : 7;

    var pointerIsHidden = !pointerSize;

    var dispatch = d3.dispatch("move");

    var moves = [];

    svg.append('rect').attr('width', window.innerWidth).attr('height', window.innerHeight).style('pointer-events', 'all').style('fill', 'none');

    var container = svg.append('g');
    var gridContainer = container.append('g');
    var pointer = container.append('circle').attr('class', color + ' pointer').attr('r', pointerSize);
    var figureContainer = container.append('g');

    var zoom = d3.zoom().scaleExtent([0.3, 3]).on('zoom', function () {
        container.attr('transform', d3.event.transform);
    });
    svg.call(zoom);
    svg.on('dblclick.zoom', null); // disable zooming on double click

    svg.on('mousemove', function () {
        var c = d3.mouse(container.node());
        var x = Math.round((c[0] - 1) / cellWidth) * cellWidth;
        var y = Math.round((c[1] - 1) / cellHeight) * cellHeight;
        pointer.attr('transform', 'translate(' + x + ', ' + y + ')');
    });
    svg.on('click', function () {
        if (!pointerIsHidden) {
            var c = d3.mouse(container.node());
            var x = Math.round((c[0] - 1) / cellWidth);
            var y = Math.round((c[1] - 1) / cellHeight);
            dispatch.call('move', document, x, y);
        }
    });

    var updateFigures = function updateFigures() {
        var all = figureContainer.selectAll('.figure').data(moves, function (d) {
            return d;
        });
        all.exit().remove();
        var enter = all.enter().append('g');
        enter.attr('class', function (d) {
            return d.color + ' figure';
        }).classed('last_move', function (d) {
            return d.lastMove;
        });
        enter.append('circle').attr('r', figureSize);
        enter.append('text').text(function (d) {
            return d.move;
        }).attr('text-anchor', 'middle').attr('y', figureSize / 2.5);

        enter.merge(all).attr('transform', function (d) {
            return 'translate(' + d.x * cellWidth + ', ' + d.y * cellHeight + ')';
        });
    };

    var updateGrid = function updateGrid() {
        var xExtent = d3.extent(moves, function (d) {
            return d.x;
        });
        var yExtent = d3.extent(moves, function (d) {
            return d.y;
        });
        var lx = Math.min(xExtent[0] || 0, 0);
        var ly = Math.min(yExtent[0] || 0, 0);
        var hx = Math.max(xExtent[1] || 0, 0);
        var hy = Math.max(yExtent[1] || 0, 0);

        var v_grid = gridContainer.selectAll('.v_grid').data(d3.range(lx - 100, hx + 101), function (d) {
            return d;
        });
        v_grid.exit().remove();
        v_grid.enter().append('line').attr('class', 'v_grid').merge(v_grid).attr('x1', function (d) {
            return d * cellWidth;
        }).attr('x2', function (d) {
            return d * cellWidth;
        }).attr('y1', (ly - 100) * cellHeight).attr('y2', (hy + 100) * cellHeight);

        var h_grid = gridContainer.selectAll('.h_grid').data(d3.range(ly - 100, hy + 101), function (d) {
            return d;
        });
        h_grid.exit().remove();
        h_grid.enter().append('line').attr('class', 'h_grid').merge(h_grid).attr('y1', function (d) {
            return d * cellHeight;
        }).attr('y2', function (d) {
            return d * cellHeight;
        }).attr('x1', (lx - 100) * cellWidth).attr('x2', (hx + 100) * cellWidth);
    };

    self.update = function () {
        updateFigures();
        updateGrid();
    };

    self.setState = function (state) {
        moves = state.hasOwnProperty('moves') ? d3.values(state.moves) : moves;
    };

    self.setPointerVisible = function () {
        pointer.attr('r', pointerSize);
        pointerIsHidden = !pointerSize;
    };

    self.setPointerInvisible = function () {
        pointer.attr('r', 0);
        pointerIsHidden = true;
    };

    self.on = function (type, listener) {
        return dispatch.on(type, listener);
    };
};

application.gameController = function (vs, status, svg, gameId) {
    status.text('Connecting...');

    var socket = io();
    socket.emit('handshake', gameId);

    var view = null;
    var currentMove = -1;
    var color = null;

    var onMove = function onMove(x, y) {
        socket.emit('move', { x: x, y: y, currentMove: currentMove });
    };

    socket.on('handshake response', function (data) {
        color = data.color;

        view = new application.gameView(svg, color);

        if (data.status !== 'New' && data.status !== 'In progress') {
            status.text(data.status);
        } else if (color != 'white' && color != 'black') {
            status.text('Joined as spectrator');
        } else {
            status.text('Joined as ' + color);
            view.on('move', onMove);
        }

        socket.emit('fetch state');

        view.setPointerInvisible();
        view.update();
    });

    socket.on('update', function (state) {
        vs.text('(' + (state.user1 || '...') + ' vs ' + (state.user2 || '...') + ')');

        view.setState(state.data);
        view.update();

        currentMove = state.data.currentMove;
        console.log(currentMove);

        if (state.status != 'In progress') {
            view.setPointerInvisible();
        } else if (state.data.currentMovePlayer === color) {
            view.setPointerVisible();
            statusSel.text('Your turn');
        } else {
            view.setPointerInvisible();
            statusSel.text('Waiting for opponent');
        }

        if (state.status === 'Finished') {
            statusSel.text('Finished');
        }
    });

    socket.on('err', function (data) {
        statusSel.text('Error: ' + data);
    });
};

//# sourceMappingURL=application.js.map