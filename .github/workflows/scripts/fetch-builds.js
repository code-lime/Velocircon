const fs = require('fs');
const util = require('util');

const readFile = util.promisify(fs.readFile);
const writeFile = util.promisify(fs.writeFile);

const invalidBuilds = new Set(['3.4.0-SNAPSHOT#491']);
const invalidVersions = new Set(['3.3.0-SNAPSHOT','3.2.0-SNAPSHOT','3.1.2-SNAPSHOT','3.1.1','3.1.1-SNAPSHOT','3.1.0','1.1.9','1.0.10']);

module.exports = async ({ github, context, core }) => {
    const userAgent = `Velocircon GitHub Actions (https://github.com/${context.repo.owner}/${context.repo.repo})`;
    const fetchJson = async url => {
        const response = await fetch(url, {
            headers: {
                Accept: 'application/json',
                'User-Agent': userAgent,
            },
        });

        if (!response.ok) {
            throw new Error(`PaperMC request failed: ${response.status} ${response.statusText} (${url})`);
        }

        return response.json();
    };

    const versionsUrl = `https://fill.papermc.io/v3/projects/velocity/versions`;
    core.startGroup(`Fetching versions from ${versionsUrl}`);

    const versionsJson = await fetchJson(versionsUrl);
    const versions = versionsJson['versions']
        .filter(v => !invalidVersions.has(v.version.id))
        .map(v => v.version.id);

    const versionBuilds = await Promise.all(versions.map(async version => {
        const buildsUrl = `https://fill.papermc.io/v3/projects/velocity/versions/${version}/builds`;
        core.info(`Fetching builds from ${buildsUrl}`);

        const buildsJson = await fetchJson(buildsUrl);
        return buildsJson
            .filter(v => v.channel === 'STABLE')
            .map(v => {
                return {
                    id: `${v.id}`,
                    version: version,
                    key: `${version}#${v.id}`,
                    url: v.downloads['server:default'].url,
                };
            });
    }));
    const builds = versionBuilds.flatMap(v => v);
    core.endGroup();

    const lastPath = '.last_builds';
    let storedBuilds = [];
    if (fs.existsSync(lastPath)) {
        storedBuilds = await readFile(lastPath, 'utf-8')
            .then(v => v
                .split('\n')
                .map(s => s.trim())
                .filter(Boolean));
    }

    const lastSet = new Set(storedBuilds);

    const newBuilds = builds.filter(v => !invalidBuilds.has(v.key) && !lastSet.has(v.key));

    if (newBuilds.length === 0) {
        core.info('No new builds found.');
        core.setOutput('matrix', '[]');
        return;
    }

    core.startGroup(`Detected ${newBuilds.length} new build(s).`);
    const buildsList = [];
    newBuilds.forEach(v => {
        lastSet.add(v.key);
        buildsList.push(v.key);
        core.info(JSON.stringify(v));
    });
    core.endGroup();

    const updated = Array.from(lastSet).sort();
    await writeFile(lastPath, `${updated.join('\n')}\n`);

    core.setOutput('matrix', JSON.stringify(newBuilds));
    core.setOutput('builds', JSON.stringify(buildsList));
};
