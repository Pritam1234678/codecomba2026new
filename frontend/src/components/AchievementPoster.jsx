/**
 * AchievementPoster — image-only, cropped, no text overlays.
 */
const IMG_MAP = {
    'Hello World':    '/achievements/achievement_poster_hello_world_50_points_final.webp',
    'Bug Hunter':     '/achievements/achievement_poster_bug_hunter_100_points_final.webp',
    'Problem Solver': '/achievements/achievement_poster_problem_solver_500_points_final.webp',
    'Algorithm Ace':  '/achievements/achievement_poster_algorithm_ace_1000_points_final.webp',
    'Code Architect': '/achievements/achievement_poster_code_architect_2000_points_final.webp',
    'Coding Legend':  '/achievements/achievement_poster_coding_legend_5000_points_final.webp',
};

export default function AchievementPoster({ tier, unlocked }) {
    const src = IMG_MAP[tier.name];
    const locked = !unlocked;
    return src ? (
        <img src={src} alt={tier.name}
            style={{
                width: '100%', height: 'auto', display: 'block',
                opacity: locked ? 0.3 : 1,
                filter: locked ? 'grayscale(1)' : 'none',
                transition: 'all 0.5s ease',
            }}
        />
    ) : null;
}
