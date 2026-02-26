const fs = require('fs');
const util = require('util');

const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

module.exports = async ({ github, context, core }) => {
    const versionsUrl = `https://fill.papermc.io/v3/projects/velocity/versions`;
    core.startGroup(`Fetching versions from ${versionsUrl}`);

    const versionsRes = await fetch(versionsUrl);
    const versionsJson = await versionsRes.json();
    const versions = versionsJson['versions']
        .filter(v => v.version.support.status === 'SUPPORTED')
        .map(v => v.version.id)

    const versionBuilds = await Promise.all(versions.map(async version => {
        const buildsUrl = `https://fill.papermc.io/v3/projects/velocity/versions/${version}/builds`;
        core.info(`Fetching builds from ${buildsUrl}`);
        
        const buildsRes = await fetch(buildsUrl);
        const buildsJson = await buildsRes.json();
        return buildsJson
            .filter(v => v.channel === 'STABLE')
            .map(v => {
                return {
                    id: `${v.id}`,
                    version: version,
                    url: v.downloads['server:default'].url,
                };
            });
    }))
    const builds = versionBuilds.flatMap(v => v);
    core.endGroup();

    const lastPath = '.last_builds';
    let lastSet = new Set();
    if (fs.existsSync(lastPath)) {
        const lines = await readFile(lastPath, 'utf-8')
            .then(v => v
                .split('\n')
                .map(s => s.trim())
                .filter(Boolean));
        lines.forEach(v => lastSet.add(v));
    }

    const newBuilds = builds.filter(v => !lastSet.has(v.id));

    if (newBuilds.length === 0) {
        core.info('No new builds found.');
        core.setOutput('matrix', '[]');
        return;
    }

    core.startGroup(`Detected ${newBuilds.length} new build(s).`);
    newBuilds.forEach(v => {
        lastSet.add(v.id);
        core.info(JSON.stringify(v));
    });
    core.endGroup();

    const updated = Array.from(lastSet).sort();
    await writeFile(lastPath, updated.join('\n') + '\n');

    core.setOutput('matrix', JSON.stringify(newBuilds));
};
