import { describe, expect, it } from 'vitest';
import { mapPost } from '@/modules/community/utils/mapPost';

describe('mapPost', () => {
  it('normalizes author, media, and matching group name while preserving post fields', () => {
    const result = mapPost(
      {
        id: 1,
        title: 'Post',
        groupId: 2,
        author: { id: 3, fullName: 'Ada', username: 'ada', avatarUrl: 'avatar.png', ignored: true },
        medias: [{ mediaType: 'IMAGE', mediaUrl: 'image.png' }],
      },
      [{ id: 2, groupName: 'Grammar' }]
    );

    expect(result).toMatchObject({
      id: 1,
      title: 'Post',
      groupName: 'Grammar',
      author: { id: 3, fullName: 'Ada', username: 'ada', avatarUrl: 'avatar.png' },
      medias: [{ type: 'image', url: 'image.png' }],
    });
  });

  it('uses empty media and group fallbacks for sparse posts', () => {
    expect(mapPost({ id: 1 }, [])).toMatchObject({
      author: { id: undefined, fullName: undefined, username: undefined, avatarUrl: undefined },
      medias: [],
      groupName: '',
    });
  });
});
