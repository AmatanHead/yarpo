const application = {};

application.gameView = function(svg, color, options) {
    const self = this;

    options = options || {};

    const cellWidth = options.hasOwnProperty('cellWidth') ? options.cellWidth : 21;
    const cellHeight = options.hasOwnProperty('cellHeight') ? options.cellHeight : 21;
    const figureSize = options.hasOwnProperty('figureSize') ? options.figureSize : 10;
    const pointerSize = options.hasOwnProperty('pointerSize') ? options.pointerSize : 7;

    let pointerIsHidden = !pointerSize;

    const dispatch = d3.dispatch("move");

    let moves = [];

    svg.append('rect')
        .attr('width', window.innerWidth)
        .attr('height', window.innerHeight)
        .style('pointer-events', 'all')
        .style('fill', 'none');

    const container = svg.append('g');
    const gridContainer = container.append('g');
    const pointer = container.append('circle').attr('class', color + ' pointer').attr('r', pointerSize);
    const figureContainer = container.append('g');

    const zoom = d3.zoom()
        .scaleExtent([0.3, 3])
        .on('zoom', () => { container.attr('transform', d3.event.transform) });
    svg.call(zoom);
    svg.on('dblclick.zoom', null);  // disable zooming on double click

    svg.on('mousemove', () => {
        const c = d3.mouse(container.node());
        const x = Math.round((c[0] - 1) / cellWidth) * cellWidth;
        const y = Math.round((c[1] - 1) / cellHeight) * cellHeight;
        pointer.attr('transform', 'translate(' + x + ', ' + y + ')')
    });
    svg.on('click', () => {
        if (!pointerIsHidden) {
            const c = d3.mouse(container.node());
            const x = Math.round((c[0] - 1) / cellWidth);
            const y = Math.round((c[1] - 1) / cellHeight);
            dispatch.call('move', document, x, y);
        }
    });

    const updateFigures = function() {
        const all = figureContainer.selectAll('.figure').data(moves, d => d);
        all.exit().remove();
        const enter = all.enter().append('g');
        enter.attr('class', d => d.color + ' figure').classed('last_move', d => d.lastMove);
        enter.append('circle').attr('r', figureSize);
        enter.append('text').text(d => d.move).attr('text-anchor', 'middle').attr('y', figureSize / 2.5);

        enter.merge(all)
             .attr('transform', d => 'translate(' + d.x * cellWidth + ', ' + d.y * cellHeight + ')');
    };

    const updateGrid = function () {
        const xExtent = d3.extent(moves, d => d.x);
        const yExtent = d3.extent(moves, d => d.y);
        const lx = Math.min(xExtent[0] || 0, 0);
        const ly = Math.min(yExtent[0] || 0, 0);
        const hx = Math.max(xExtent[1] || 0, 0);
        const hy = Math.max(yExtent[1] || 0, 0);

        const v_grid = gridContainer.selectAll('.v_grid').data(d3.range(lx - 100, hx + 101), d => d);
        v_grid.exit().remove();
        v_grid.enter().append('line').attr('class', 'v_grid')
            .merge(v_grid)
              .attr('x1', d => d * cellWidth)
              .attr('x2', d => d * cellWidth)
              .attr('y1', (ly - 100) * cellHeight)
              .attr('y2', (hy + 100) * cellHeight);

        const h_grid = gridContainer.selectAll('.h_grid').data(d3.range(ly - 100, hy + 101), d => d);
        h_grid.exit().remove();
        h_grid.enter().append('line').attr('class', 'h_grid')
              .merge(h_grid)
              .attr('y1', d => d * cellHeight)
              .attr('y2', d => d * cellHeight)
              .attr('x1', (lx - 100) * cellWidth)
              .attr('x2', (hx + 100) * cellWidth);
    };

    self.update = function() {
        updateFigures();
        updateGrid();
    };

    self.setState = function(state) {
        moves = state.hasOwnProperty('moves') ? d3.values(state.moves) : moves;
    };

    self.setPointerVisible = function() {
        pointer.attr('r', pointerSize);
        pointerIsHidden = !pointerSize;
    };

    self.setPointerInvisible = function() {
        pointer.attr('r', 0);
        pointerIsHidden = true;
    };

    self.on = function(type, listener) {
        return dispatch.on(type, listener);
    };
};


application.gameController = function(vs, status, svg, gameId) {
    status.text('Connecting...');

    const socket = io();
    socket.emit('handshake', gameId);

    let view = null;
    let currentMove = -1;
    let color = null;

    const onMove = function(x, y) {
        socket.emit('move', {x: x, y: y, currentMove: currentMove})
    };

    socket.on('handshake response', (data) => {
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

    socket.on('update', (state) => {
        vs.text('(' + (state.user1 || '...') + ' vs ' + (state.user2 || '...') + ')');

        view.setState(state.data);
        view.update();

        currentMove = state.data.currentMove;

        if (state.status != 'In progress') {
            view.setPointerInvisible();
        } else if (state.data.currentMovePlayer === color) {
            view.setPointerVisible();
            statusSel.text('Your turn');
        } else {
            view.setPointerInvisible();
            if (color === 'black' || color === 'white') {
                statusSel.text('Waiting for opponent');
            }
        }

        if (state.status === 'Finished') {
            statusSel.text('Finished');
        }
    });

    socket.on('err', (data) => {
        statusSel.text('Error: ' + data);
    })
};
